//package com.example.signuplogina
//
//import GalleryItem
//import android.Manifest
//import com.example.signuplogina.GalleryAdapter
//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.GridLayoutManager
//import com.example.signuplogina.databinding.FragmentGalleryBinding
//import java.util.ArrayList
//
//class GalleryFragment : Fragment() {
//    private var _binding: FragmentGalleryBinding? = null
//    private val binding get() = _binding!!
//    lateinit var mContext: Context
//
//    // List to store GalleryItems and Selected Images
//    var images = ArrayList<GalleryItem>()
//    var selectedImages = ArrayList<GalleryItem>()
//    lateinit var galleryAdapter: GalleryAdapter
//    val pickImages =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//
//                result.data?.let { data ->
//                    if (data.clipData != null) {
//                        val mClipData = data.clipData
//                        for (i in 0 until mClipData!!.itemCount) {
//                            val item = mClipData.getItemAt(i)
//                            val uri = item.uri
//                            images?.add(GalleryItem(uri))
//                        }
//                    } else if (data.data != null) {
//                        val uri = data.data
//                        images?.add(GalleryItem(uri!!))
//                    }
//                }
//                galleryAdapter.setData(images)
//            }
//        }
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        if (permissions.all { it.value }) {
//            Toast.makeText(mContext, "Permissions granted", Toast.LENGTH_SHORT).show()
//            openImageChooser()
//        } else {
//            Toast.makeText(mContext, "Permissions are not granted", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        mContext = context
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        permission()
//        imageAdapter()
//    }
//
//    private fun permission() {
//        if (ContextCompat.checkSelfPermission(
//                mContext,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            getImage()
//        } else {
//            requestPermission()
//        }
//    }
//
//    private fun requestPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(
//                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                100
//            )
//        }
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            // Do what you want to do
//            getImage()
//        } else {
//            requestPermission()
//        }
//    }
//
//    fun imageAdapter() {
//        val galleryAdapter = GalleryAdapter(this)
//        binding.exampleRv.layoutManager = GridLayoutManager(context, 4)
//        binding.exampleRv.adapter = galleryAdapter
//
//        binding.selectButton.setOnClickListener(View.OnClickListener {
//            val list = galleryAdapter.getSelectedImages()
//            sendResult(list)
//        })
//        this.galleryAdapter = galleryAdapter
//    }
//
//    private fun sendResult(arrayList: ArrayList<GalleryItem>) {
//        var arrayURLS =  arrayListOf<String>()
//        for(item in arrayList){
//            arrayURLS.add(item.uri.toString())
//        }
//
//        val bundle = androidx.core.os.bundleOf("selectedImages" to arrayURLS.toTypedArray())
//        findNavController().previousBackStackEntry?.savedStateHandle?.set("key", bundle)
//        findNavController().popBackStack()
//    }
//
//    private fun getImage() {
//        val intent = Intent()
//        intent.type = "image/*"
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        intent.action = Intent.ACTION_GET_CONTENT
//        pickImages.launch(intent)
//    }
//
//    private fun openImageChooser() {
//        val intent = Intent()
//        intent.type = "image/*"
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        intent.action = Intent.ACTION_GET_CONTENT
//        pickImages.launch(intent)
//    }
//    /*if (result.resultCode == Activity.RESULT_OK) {
//     */
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//    companion object {
//        fun newInstance(): GalleryFragment {
//            return GalleryFragment()
//        }
//        private const val TAG = "GalleryFragment"
//    }
//}