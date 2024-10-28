package com.example.myappkotlin


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myappkotlin.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!! // This will give you access to the binding instance

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: ClassificationDatabaseHelper
    private lateinit var adapter: classificationAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = ClassificationDatabaseHelper(requireContext())
        adapter = classificationAdapter(db.getClassifications(), requireContext())

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


        binding.savedataBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Request WRITE_EXTERNAL_STORAGE permission only for Android 9 and below
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

            } else {
                // Call the export method directly if permission is granted or on Android 10+
                db.exportToSQLiteFile(requireContext(), "exported_data${System.currentTimeMillis()}")
            }
        }



    }

    override fun onResume() {
        super.onResume()
        adapter.refreshData(db.getClassifications())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up the binding reference
    }




}


