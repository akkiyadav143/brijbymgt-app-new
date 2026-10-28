package com.brijby77.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brijby77.R
import com.brijby77.data.entity.NoteEntity
import com.brijby77.databinding.FragmentNoteDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(requireActivity().application)
    }

    private var currentNote: NoteEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val noteId = arguments?.getLong("noteId", -1L) ?: -1L

        loadNote(noteId)

        binding.btnEditNote.setOnClickListener {
            currentNote?.let { note ->
                val bundle = Bundle().apply { putLong("noteId", note.id) }
                findNavController().navigate(R.id.action_note_detail_to_add_edit_note, bundle)
            }
        }

        binding.btnTogglePinDetail.setOnClickListener {
            currentNote?.let { note ->
                viewModel.togglePin(note)
                loadNote(note.id)
            }
        }

        binding.btnDeleteNoteDetail.setOnClickListener {
            currentNote?.let { note ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete '${note.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteNote(note)
                        Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun loadNote(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val note = viewModel.getNoteById(id)
            currentNote = note
            if (note != null) {
                binding.tvDetailTitle.text = note.title.ifBlank { getString(com.brijby77.R.string.untitled_note) }
                binding.tvDetailContent.text = note.content

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                binding.tvCreatedDate.text = getString(com.brijby77.R.string.created_format, sdf.format(Date(note.createdAt)))
                binding.tvUpdatedDate.text = getString(com.brijby77.R.string.updated_format, sdf.format(Date(note.updatedAt)))

                if (note.isPinned) {
                    binding.tvPinStatus.visibility = View.VISIBLE
                    binding.tvPinStatus.text = getString(com.brijby77.R.string.pinned_status)
                    binding.btnTogglePinDetail.text = getString(com.brijby77.R.string.unpin_action)
                } else {
                    binding.tvPinStatus.visibility = View.GONE
                    binding.btnTogglePinDetail.text = getString(com.brijby77.R.string.pin_action)
                }
            } else {
                Toast.makeText(requireContext(), getString(com.brijby77.R.string.note_not_found), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

