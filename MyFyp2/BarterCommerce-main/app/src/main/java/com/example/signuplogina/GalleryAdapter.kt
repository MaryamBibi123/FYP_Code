//package com.example.signuplogina
//
//import GalleryItem
//import android.content.Context
//import android.graphics.Bitmap
//import android.net.Uri
//import android.provider.MediaStore
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.Toast
//import androidx.core.content.ContentProviderCompat.requireContext
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.example.signuplogina.databinding.GalleryItemBinding
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.IOException
//import java.lang.ref.WeakReference
//import java.util.ArrayList
//import java.util.Collections
//
//class GalleryAdapter(fragment: Fragment) :
//    RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {
//    private val selectedImages = ArrayList<GalleryItem>()
//    val exampleList: ArrayList<GalleryItem> = ArrayList()
//    private val mFragment: Fragment = fragment
//    fun setData(list: ArrayList<GalleryItem>) {
//        exampleList.clear()
//        exampleList.addAll(list)
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
//        val itemView =
//            GalleryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return GalleryViewHolder(itemView)
//    }
//
//    override fun getItemCount(): Int {
//        return exampleList.size
//    }
//
//    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
//        val currentItem = exampleList[position]
//
//        //Glide.with(holder.binding.galleryItemImage.context)
//        //    .load(currentItem.uri)
//        //    .into(holder.binding.galleryItemImage)
//
//        mFragment.lifecycleScope.launch {
//            try {
////                val bitmap = withContext(Dispatchers.IO) {
//                    MediaStore.Images.Media.getBitmap(
//                        holder.binding.galleryItemImage.context.contentResolver,
//                        currentItem.uri
//                    )
//                }
//                holder.binding.galleryItemImage.setImageBitmap(bitmap)
//            } catch (e: IOException) {
//                Log.e(TAG, "Error loading image", e)
//                //holder.binding.galleryItemImage.setImageResource(R.drawable.error_placeholder)
//            }
//        }
//        holder.itemView.setOnClickListener {
//            if (selectedImages.contains(currentItem)) {
//                selectedImages.remove(currentItem)
//                Log.v("value: ", "remove")
//                holder.itemView.alpha = 1f
//            } else {
//                if(selectedImages.size <= 3){
//                    selectedImages.add(currentItem)
//                    holder.itemView.alpha = 0.5f
//                } else {
//                    Toast.makeText(
//                        holder.binding.galleryItemImage.context,
//                        "You already pick 3 photos",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//
//            }
//        }
//    }
//
//    inner class GalleryViewHolder(val binding: GalleryItemBinding) : RecyclerView.ViewHolder(binding.root)
//
//    fun getSelectedImages(): ArrayList<GalleryItem> {
//        return selectedImages
//    }
//
//    companion object {
//        private const val TAG = "GalleryAdapter"
//    }
//}