package com.example.signuplogina.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.Utils
import com.example.signuplogina.adapter.PollAdapter
import com.example.signuplogina.mvvm.PollViewModel
import com.example.signuplogina.databinding.FragmentGeneralPoolBinding

class GeneralPollFragment : Fragment() {

    private var _binding: FragmentGeneralPoolBinding? = null
    private val binding get() = _binding!!

    private lateinit var pollAdapter: PollAdapter
    private val viewModel: PollViewModel by viewModels()
    val currentUserId = Utils.getUidLoggedIn()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneralPoolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observePolls()

//        binding.fabAddPoll.setOnClickListener {
//            val intent = Intent(requireContext(), AddPollActivity::class.java)
//            startActivity(intent)
//        }
    }

    private fun setupRecyclerView() {
        pollAdapter = PollAdapter(
            emptyList(),
            onPollClick = { /* Handle poll click if needed */ },
            onOptionSelected = { poll, selectedOption ->
                viewModel.voteInPoll(poll, selectedOption, category = "General")
            },
            currentUserId
        )
        binding.pollRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.pollRecyclerView.adapter = pollAdapter
    }

    private fun observePolls() {
        viewModel.polls.observe(viewLifecycleOwner) { polls ->
            pollAdapter.updateList(polls)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
