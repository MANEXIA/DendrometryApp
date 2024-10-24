package com.example.myappkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myappkotlin.databinding.FragmentGuideBinding


class GuideFragment : Fragment() {
    private var _binding: FragmentGuideBinding? = null
    private val binding get() = _binding!! // This will give you access to the binding instance

    private lateinit var guideTxt: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup the RecyclerView


        guideTxt = binding.Stepstxt
        binding.guideLeftBtn.setOnClickListener{
            if(guideTxt.text == "Steps"){
                guideTxt.text = "Conditions"
                binding.conditionscontainer.visibility = View.VISIBLE
                binding.stepscontainer.visibility = View.GONE
            }else{
                guideTxt.text = "Steps"
                binding.conditionscontainer.visibility = View.GONE
                binding.stepscontainer.visibility = View.VISIBLE
            }
        }

        binding.guideRightBtn.setOnClickListener{
            if(guideTxt.text == "Steps"){
                guideTxt.text = "Conditions"
                binding.conditionscontainer.visibility = View.VISIBLE
                binding.stepscontainer.visibility = View.GONE
            }else{
                guideTxt.text = "Steps"
                binding.conditionscontainer.visibility = View.GONE
                binding.stepscontainer.visibility = View.VISIBLE
            }
        }



    }

}