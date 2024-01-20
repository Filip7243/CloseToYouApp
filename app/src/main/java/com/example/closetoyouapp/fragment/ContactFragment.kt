package com.example.closetoyouapp.fragment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.closetoyouapp.AppDatabase
import com.example.closetoyouapp.ContactAdapter
import com.example.closetoyouapp.ContactPhoto
import com.example.closetoyouapp.HomeActivity
import com.example.closetoyouapp.Localization
import com.example.closetoyouapp.MyApp
import com.example.closetoyouapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val USER_FRIENDS_ARG = "userFriends"

class ContactFragment : Fragment(), ContactAdapter.OnChangePhotoListener {
    private var userFriends: ArrayList<Localization>? = null

    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

//    private var localContactsMap = mutableMapOf<String, String>()
    private var currentPhoneNumber: String? = null

    private val IMAGE_PICK_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002

    private val PERMISSIONS_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userFriends = it.getParcelableArrayList(USER_FRIENDS_ARG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_contact, container, false)

        initializeUI(rootView)

        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA).forEach {
            if (ContextCompat.checkSelfPermission(requireActivity(), it) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(it), PERMISSIONS_REQUEST_CODE)
            }
        }

        displayContacts(userFriends as ArrayList<Localization>)
        loadPhotosIntoMapAndAdapter()

        return rootView
    }

    private fun initializeUI(rootView: View) {
        database = MyApp.getDatabase(requireActivity())
        recyclerView = rootView.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(rootView.context)
        recyclerView.adapter = ContactAdapter(this)

        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener { refreshData() }

        loadPhotosIntoAdapter(recyclerView.adapter as ContactAdapter)
    }

    private fun refreshData() {
        swipeRefreshLayout.isRefreshing = false
    }

    private fun loadPhotosIntoAdapter(adapter: ContactAdapter) {
        CoroutineScope(Dispatchers.IO).launch {
            val photoMap = database.contactPhotoDao().getAllPhotos()
                .associateBy({ it.phoneNumber }, { it.photoUri })
            Log.d("ContactActivity", "Wczytane zdjęcia z bazy danych: $photoMap")
            withContext(Dispatchers.Main) {
                adapter.setPhotoPaths(photoMap)
            }
        }
    }

    private fun displayContacts(contactsFromBackend: List<Localization>) {
        println("DisplayContcat = $contactsFromBackend")
        val contactsToShow = contactsFromBackend.mapNotNull {
            val name = HomeActivity.localContactsMap[it.phoneNumber]
            if (name != null && it.hasPermission) Localization(
                name,
                it.phoneNumber,
                it.latitude,
                it.longitude,
                it.hasPermission,
                it.updatedAt
            )
            else null
        }

        println("contactsToShow = $contactsToShow")

        val adapter = recyclerView.adapter as? ContactAdapter
        adapter?.updateContacts(contactsToShow) ?: Log.e(
            "ContactActivity",
            "Adapter is not set or wrong type"
        )
    }

    private fun loadPhotosIntoMapAndAdapter() {
        CoroutineScope(Dispatchers.IO).launch {
            val photoMap = database.contactPhotoDao().getAllPhotos().associateBy({ it.phoneNumber }, { it.photoUri })
            withContext(Dispatchers.Main) {
                (activity as? HomeActivity)?.updateContactPhotosMap(photoMap)
                (recyclerView.adapter as? ContactAdapter)?.setPhotoPaths(photoMap)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    private fun takePhoto() {
        saveAppState()
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            Log.d("ContactActivity", "takePhoto - Intencja zrobienia zdjęcia uruchomiona")
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
        } else {
            Log.d("ContactActivity", "takePhoto - Błąd uruchomienia intencji")
        }
    }

    private fun saveAppState() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("App_State", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("currentPhoneNumber", currentPhoneNumber)
        editor.apply()
    }

    override fun onChangePhotoRequested(phoneNumber: String) {
        currentPhoneNumber = phoneNumber
        val chooseIntent = AlertDialog.Builder(requireActivity())
        chooseIntent.setTitle("Wybierz akcję")
        chooseIntent.setItems(arrayOf("Wybierz z galerii", "Zrób zdjęcie")) { dialog, which ->
            when (which) {
                0 -> openGallery()
                1 -> takePhoto()
            }
        }
        chooseIntent.show()
    }

    override fun onResume() {
        super.onResume()
        restoreAppState()
    }

    private fun restoreAppState() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("App_State", AppCompatActivity.MODE_PRIVATE)
        currentPhoneNumber = sharedPreferences.getString("currentPhoneNumber", null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMAGE_PICK_CODE -> if (resultCode == Activity.RESULT_OK) {
                Log.d("ContactActivity", "onActivityResult - Zdjęcie zrobione")
                data?.data?.let { uri ->
                    handleSelectedImage(uri)
                }
            } else {
                Log.d("ContactActivity", "onActivityResult - Zdjęcie nie zostało zrobione")
            }

            CAMERA_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                val photo = data?.extras?.get("data") as? Bitmap
                photo?.let {
                    val photoUri = saveImageToGallery(it)
                    photoUri?.let { uri ->
                        handleSelectedImage(uri)
                    }
                }
            }
        }
    }

    private fun handleSelectedImage(imageUri: Uri) {
        val realPath = getRealPathFromURI(requireActivity(), imageUri)
        Log.d("ContactActivity", "handleSelectedImage - Real Path: $realPath")
        val phoneNumber = currentPhoneNumber ?: return

        if (realPath != null) {
            val photoPathsFromSharedPreferences =
                getPhotoPathsFromSharedPreferences(requireActivity())
            savePhotoPathsToSharedPreferences(
                requireActivity(),
                photoPathsFromSharedPreferences + (phoneNumber to realPath)
            )

            CoroutineScope(Dispatchers.IO).launch {
                val existingPhoto = database.contactPhotoDao().getPhotoByPhoneNumber(phoneNumber)
                if (existingPhoto != null) {
                    existingPhoto.photoUri = realPath
                    database.contactPhotoDao().updatePhoto(existingPhoto)
                } else {
                    val contactPhoto = ContactPhoto(phoneNumber, realPath)
                    database.contactPhotoDao().insertPhoto(contactPhoto)
                }

                withContext(Dispatchers.Main) {
                    updateRecyclerView()
                }
            }
        }
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor != null && cursor.moveToFirst()) {
                return columnIndex?.let { cursor.getString(it) }
            }
        } catch (e: Exception) {
            Log.e("getRealPathFromURI", "ERROR WHEN GETTING REAL PATH: ${e.message}")
        } finally {
            cursor?.close()
        }

        return null
    }

    private fun getPhotoPathsFromSharedPreferences(context: Context): Map<String, String> {
        val sharedPreferences = context.getSharedPreferences("photo_paths", Context.MODE_PRIVATE)
        return sharedPreferences.all as Map<String, String>
    }

    private fun savePhotoPathsToSharedPreferences(context: Context, paths: Map<String, String>) {
        val sharedPreferences = context.getSharedPreferences("photo_paths", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        for ((phoneNumber, photoPath) in paths) {
            editor.putString(phoneNumber, photoPath)
        }
        editor.apply()
    }

    private fun updateRecyclerView() {
        CoroutineScope(Dispatchers.IO).launch {
            val photoMap = database.contactPhotoDao().getAllPhotos()
                .associateBy({ it.phoneNumber }, { it.photoUri })
            withContext(Dispatchers.Main) {
                (recyclerView.adapter as? ContactAdapter)?.apply {
                    setPhotoPaths(photoMap)
                    notifyDataSetChanged()
                }
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }
        Log.d("ContactActivity", "saveImageToGallery - ContentValues utworzone")

        return try {
            val uri = requireActivity().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            Log.d("ContactActivity", "saveImageToGallery - Zdjęcie zapisane, URI: $uri")
            requireActivity().contentResolver.openOutputStream(uri ?: return null).use { out ->
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            }
            uri
        } catch (e: Exception) {
            Log.e("ContactActivity", "saveImageToGallery - Błąd zapisu zdjęcia: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ContactFragment.
         */
        @JvmStatic
        fun newInstance(userFriends: ArrayList<Localization>) =
            ContactFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(USER_FRIENDS_ARG, userFriends)
                }
            }
    }
}