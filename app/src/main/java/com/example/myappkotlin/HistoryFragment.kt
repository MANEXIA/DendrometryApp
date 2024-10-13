package com.example.myappkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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


