package com.brijby77.ui.notes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijby77.R
import com.brijby77.databinding.FragmentNotesListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesListFragment : Fragment() {

    private var _binding: FragmentNotesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(requireActivity().application)
    }

    private lateinit var noteAdapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeNotes()
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onNoteClick = { note ->
                val bundle = Bundle().apply { putLong("noteId", note.id) }
                findNavController().navigate(R.id.action_notes_list_to_note_detail, bundle)
            },
            onPinClick = { note ->
                viewModel.togglePin(note)
            },
            onDeleteClick = { note ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete '${note.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteNote(note)
                        Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.rvNotesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noteAdapter
        }
    }

    private fun setupListeners() {
        binding.etSearchNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.fabAddNote.setOnClickListener {
            val bundle = Bundle().apply { putLong("noteId", -1L) }
            findNavController().navigate(R.id.action_notes_list_to_add_edit_note, bundle)
        }
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notesList.collectLatest { notes ->
                noteAdapter.submitList(notes)
                if (notes.isEmpty()) {
                    binding.rvNotesList.visibility = View.GONE
                    binding.tvEmptyNotes.visibility = View.VISIBLE
                } else {
                    binding.rvNotesList.visibility = View.VISIBLE
                    binding.tvEmptyNotes.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

