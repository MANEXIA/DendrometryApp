package com.example.Dendrometry.ui


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Dendrometry.dbmshelpers.ClassificationAdapter
import com.example.Dendrometry.dbmshelpers.ClassificationDatabaseHelper
import com.example.Dendrometry.databinding.FragmentHistoryBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!! // This will give you access to the binding instance

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: ClassificationDatabaseHelper
    private lateinit var adapter: ClassificationAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = ClassificationDatabaseHelper(requireContext())
        adapter = ClassificationAdapter(db.getClassifications(getUsername()), requireContext())

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup the RecyclerView
        binding.HistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.HistoryRecyclerView.adapter = adapter

        // Access the SharedPreferences where the data was saved
        val sharedPreferences = requireActivity().getSharedPreferences("userSession", Context.MODE_PRIVATE)
        // Retrieve the stored values
        val loggedInUsername = sharedPreferences.getString("loggedInUsername", null)
        val loggedInName = sharedPreferences.getString("loggedInName", null)


        binding.savedataBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Request WRITE_EXTERNAL_STORAGE permission only for Android 9 and below
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

            } else {
                // Get current date and time
                val currentDateTime = LocalDateTime.now()
                // Define a date-time formatter to format the output
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // Customize the format as needed
                // Call the export method directly if permission is granted or on Android 10+
                db.exportToExcelFile(requireContext(), "${loggedInName}_Classification_History(${currentDateTime.format(formatter)})", "${loggedInUsername}")
            }
        }

    }

    override fun onResume() {
        super.onResume()
        adapter.refreshData(db.getClassifications(getUsername()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up the binding reference
    }

    private fun getUsername():String{
        val sharedPreferences = requireActivity().getSharedPreferences("userSession", MODE_PRIVATE)
        val loggedInUsername = sharedPreferences.getString("loggedInUsername", null)
        return "$loggedInUsername"
    }



}


