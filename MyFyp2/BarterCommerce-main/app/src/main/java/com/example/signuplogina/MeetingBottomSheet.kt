package com.example.signuplogina

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import com.example.signuplogina.activities.SelectLocationActivity
import com.example.signuplogina.databinding.BottomSheetMeetingPlanBinding

class MeetingBottomSheet(
    private val onSave: (location: String, date: String, time: String) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private var initialLocation: String? = null
    private var initialDate: String? = null
    private var initialTime: String? = null

    private lateinit var binding: BottomSheetMeetingPlanBinding

    companion object {
        fun newInstance(
            location: String?,
            date: String?,
            time: String?,
            onSave: (String, String, String) -> Unit
        ): MeetingBottomSheet {
            val fragment = MeetingBottomSheet(onSave)
            val args = Bundle()
            args.putString("location", location)
            args.putString("date", date)
            args.putString("time", time)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialLocation = it.getString("location")
            initialDate = it.getString("date")
            initialTime = it.getString("time")
            selectedDate = initialDate
            selectedTime = initialTime
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetMeetingPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val selectLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0)
            val lon = result.data?.getDoubleExtra("lon", 0.0)
            val address = result.data?.getStringExtra("address")
            binding.btnOpenMap.text = address
            binding.etLocation.setText(address)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etLocation = view.findViewById<TextInputEditText>(R.id.etLocation)
        val btnOpenMap = view.findViewById<Button>(R.id.btnOpenMap)
        val btnSelectDate = view.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = view.findViewById<Button>(R.id.btnSelectTime)
        val btnSave = view.findViewById<Button>(R.id.btnSaveMeeting)

        initialLocation?.takeIf { it.isNotEmpty() }?.let {
            etLocation.setText(it)
            btnOpenMap.text = "Location: $it"
        }

        initialDate?.takeIf { it.isNotEmpty() }?.let {
            btnSelectDate.text = "Date: $it"
        }

        initialTime?.takeIf { it.isNotEmpty() }?.let {
            btnSelectTime.text = "Time: $it"
        }

//        // Set initial values if available
//        initialLocation?.let {
//            etLocation.setText(it)
//            btnOpenMap.text = it
//        }
//
//        initialDate?.let {
//            btnSelectDate.text = "Date: $it"
//        }
//
//        initialTime?.let {
//            btnSelectTime.text = "Time: $it"
//        }

        btnOpenMap.setOnClickListener {
            val intent = Intent(requireContext(), SelectLocationActivity::class.java)
            selectLocationLauncher.launch(intent)
        }

        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDate = "$day/${month + 1}/$year"
                btnSelectDate.text = "Date: $selectedDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                btnSelectTime.text = "Time: $selectedTime"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        btnSave.setOnClickListener {
            val location = etLocation.text.toString()
            if (location.isNotBlank() && selectedDate != null && selectedTime != null) {
                onSave(location, selectedDate!!, selectedTime!!)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
