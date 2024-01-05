package com.example.closetoyou.fragment

import com.example.closetoyou.ContactPhoto
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
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.closetoyou.R
import com.example.closetoyou.AppDatabase
import com.example.closetoyou.ContactAdapter
import com.example.closetoyou.HomeActivity
import com.example.closetoyou.HomeActivity.Companion.API_URL
import com.example.closetoyou.Localization
import com.example.closetoyou.MyApp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.Instant

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val CONTACT_LIST = "contactList"

/**
 * A simple [Fragment] subclass.
 * Use the [ContactFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ContactFragment : Fragment(), ContactAdapter.OnChangePhotoListener {
    // TODO: Rename and change types of parameters
    private var contactList: ArrayList<Localization>? = null

    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, JsonDeserializer { json, _, _ ->
            Instant.parse(json.asJsonPrimitive.asString)
        })
        .create()
    private var localContactsMap = mutableMapOf<String, String>()
    private var currentPhoneNumber: String? = null

    private val IMAGE_PICK_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            contactList = it.getParcelableArrayList(CONTACT_LIST)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_contact, container, false)

        initializeUI(rootView)
        return rootView
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
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ContactFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    private fun initializeUI(rootView: View) {
        database = MyApp.getDatabase(requireActivity().applicationContext)
        recyclerView = rootView.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(rootView.context)
        recyclerView.adapter = ContactAdapter(this, database)

        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener { refreshData() }

        loadPhotosIntoAdapter(recyclerView.adapter as ContactAdapter)
    }


    private fun refreshData() {
        swipeRefreshLayout.isRefreshing = false
    }

    private fun loadPhotosIntoAdapter(adapter: ContactAdapter) {
        CoroutineScope(Dispatchers.IO).launch {
            val photoMap = database.contactPhotoDao().getAllPhotos().associateBy({ it.phoneNumber }, { it.photoUri })
            Log.d("ContactActivity", "Wczytane zdjęcia z bazy danych: $photoMap")
            withContext(Dispatchers.Main) {
                adapter.setPhotoPaths(photoMap)
            }
        }
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val column_index = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor != null && cursor.moveToFirst()) {
                return column_index?.let { cursor.getString(it) }
            }
        } catch (e: Exception) {
            Log.e("ContactActivity", "Błąd podczas uzyskiwania ścieżki: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun savePhotoPathsToSharedPreferences(context: Context, paths: Map<String, String>) {
        val sharedPreferences = context.getSharedPreferences("photo_paths", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        for ((phoneNumber, photoPath) in paths) {
            editor.putString(phoneNumber, photoPath)
        }
        editor.apply()
    }


    private fun getPhotoPathsFromSharedPreferences(context: Context): Map<String, String> {
        val sharedPreferences = context.getSharedPreferences("photo_paths", Context.MODE_PRIVATE)
        return sharedPreferences.all as Map<String, String>
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Anulowanie wszystkich uruchomionych coroutine
    }

    private fun sendNumbersToBackend(numberList: List<String>) {
        println("numery: $numberList")
        val json = gson.toJson(numberList)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()

        HomeActivity.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace() // Log the error
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val type = object : TypeToken<List<Localization>>() {}.type
                    val contacts: List<Localization> = gson.fromJson(body, type)
                    displayContacts(contacts)
                }
            }
        })
    }

    private fun getContacts(rootView: View) {
        val contextResolver = requireActivity().contentResolver
        val cursor = contextResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        val numberList = mutableListOf<String>()
        cursor?.use {
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                localContactsMap[number] = name
                numberList.add(number)
            }
        }

        sendNumbersToBackend(numberList)
    }

    private fun displayContacts(contactsFromBackend: List<Localization>) {
        val contactsToShow = contactsFromBackend.mapNotNull {
            val name = localContactsMap[it.phoneNumber]
            if (name != null && it.hasPermission) Localization(
                name,
                it.phoneNumber,
                it.latitude,
                it.longitude,
                it.hasPermission,
            )
            else null
        }

        val adapter = recyclerView.adapter as? ContactAdapter
        adapter?.updateContacts(contactsToShow) ?: Log.e("ContactActivity", "Adapter is not set or wrong type")
    }


    override fun onChangePhotoRequested(phoneNumber: String) {
        currentPhoneNumber = phoneNumber
        val chooseIntent = AlertDialog.Builder(this.context)
        chooseIntent.setTitle("Wybierz akcję")
        chooseIntent.setItems(arrayOf("Wybierz z galerii", "Zrób zdjęcie")) { dialog, which ->
            when (which) {
                0 -> openGallery()
                1 -> takePhoto()
            }
        }
        chooseIntent.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    private fun handleSelectedImage(imageUri: Uri) {
        val realPath = getRealPathFromURI(this.requireContext(), imageUri)
        Log.d("ContactActivity", "handleSelectedImage - Real Path: $realPath")
        val phoneNumber = currentPhoneNumber ?: return

        if (realPath != null) {
            val photoPathsFromSharedPreferences = getPhotoPathsFromSharedPreferences(this.requireContext())
            savePhotoPathsToSharedPreferences(this.requireContext(), photoPathsFromSharedPreferences + (phoneNumber to realPath))

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
        //TODO: add shared prefrences to params
        val sharedPreferences = requireActivity().getSharedPreferences("App_State", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("currentPhoneNumber", currentPhoneNumber)
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        restoreAppState()
    }

    private fun restoreAppState() {
        val sharedPreferences = requireActivity().getSharedPreferences("App_State", AppCompatActivity.MODE_PRIVATE)
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

            val uri = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
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


    private fun updateRecyclerView() {
        CoroutineScope(Dispatchers.IO).launch {
            val photoMap = database.contactPhotoDao().getAllPhotos().associateBy({ it.phoneNumber }, { it.photoUri })
            withContext(Dispatchers.Main) {
                (recyclerView.adapter as? ContactAdapter)?.apply {
                    setPhotoPaths(photoMap)
                    notifyDataSetChanged()
                }
            }
        }
    }
}