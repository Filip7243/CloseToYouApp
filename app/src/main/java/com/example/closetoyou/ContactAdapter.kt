package com.example.closetoyou

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import java.io.File

class ContactAdapter(
    private val listener: OnChangePhotoListener,
    private val database: AppDatabase
) : RecyclerView.Adapter<ContactAdapter.MyViewHolder>() {

    private var contacts: List<Localization> = listOf()
    private var photos: List<ContactPhoto> = listOf()
    private var photoPaths: Map<String, String> = emptyMap()

    fun updateContacts(newContacts: List<Localization>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    fun setPhotoPaths(paths: Map<String, String>) {
        photoPaths = paths
        notifyDataSetChanged()
        println("sciezki:" + paths)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_row, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.number.text = contact.phoneNumber

        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.WHITE)
        } else {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        }

        holder.lastSeenTextView.text = contact.getLastSeenText();

        holder.buttonChangeImage.setOnClickListener {
            contact.phoneNumber?.let { it1 -> listener.onChangePhotoRequested(it1) }
        }


        val photoPath = photoPaths[contact.phoneNumber]
        Log.d("ContactAdapter", "onBindViewHolder - Photo Path: $photoPath")

        if (!photoPath.isNullOrEmpty()) {
            // Ładowanie zdjęcia bezpośrednio z lokalnej ścieżki pliku
            val imgFile = File(photoPath)
            if (imgFile.exists()) {
                Log.d("ContactAdapter", "Plik istnieje: ${imgFile.absolutePath}")
                Glide.with(holder.itemView.context)
                    .load(photoPath)
                    .into(holder.contactImage)
            }
        } else {
            holder.contactImage.setImageResource(R.drawable.background_main)
        }

        Log.d(
            "ContactAdapter",
            "Ładowanie zdjęcia dla numeru: ${contact.phoneNumber}, ścieżka: ${photoPath}"
        )
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.contact_name)
        val number: TextView = view.findViewById(R.id.contact_number)
        val buttonChangeImage: Button = view.findViewById(R.id.button_change_image)
        val contactImage: ShapeableImageView = view.findViewById(R.id.contact_image)
        val lastSeenTextView: TextView = view.findViewById(R.id.last_seen_text_view)
    }

    interface OnChangePhotoListener {
        fun onChangePhotoRequested(phoneNumber: String)
    }
}
