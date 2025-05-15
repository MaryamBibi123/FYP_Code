package com.example.signuplogina.fragments

import AskTradeBattleBottomSheet
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.signuplogina.adapter.TradeBattleAdapter
import com.example.signuplogina.databinding.FragmentTradeBattleBinding
import com.example.signuplogina.mvvm.PollViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TradeBattleFragment : Fragment() {

    private var _binding: FragmentTradeBattleBinding? = null
    private val binding get() = _binding!!

    private val pollViewModel: PollViewModel by viewModels()

    private lateinit var tradeBattleAdapter: TradeBattleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTradeBattleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeTradeBattles()

//        binding.fabAddTradeBattle.setOnClickListener {
//            val bottomSheet = AskTradeBattleBottomSheet()
//            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
//        }
    }

    private fun setupRecyclerView() {
        tradeBattleAdapter = TradeBattleAdapter()
        binding.TradeBattleRecyclerView.apply {

            layoutManager = LinearLayoutManager(requireContext(),  LinearLayoutManager.VERTICAL, false)
            adapter = tradeBattleAdapter
        }
    }

    private fun observeTradeBattles() {
        pollViewModel.tradeBattles.observe(viewLifecycleOwner) { battles ->
            tradeBattleAdapter.submitList(battles)
            battles.forEach { tradeBattle ->
                tradeBattleAdapter.setBattleId(tradeBattle.battleId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
