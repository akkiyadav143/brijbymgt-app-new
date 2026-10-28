package com.brijby77.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brijby77.databinding.FragmentAddEditNoteBinding
import kotlinx.coroutines.launch

class AddEditNoteFragment : Fragment() {

    private var _binding: FragmentAddEditNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(requireActivity().application)
    }

    private var noteId: Long = -1L
    private var createdAt: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noteId = arguments?.getLong("noteId", -1L) ?: -1L

        if (noteId != -1L) {
            binding.tvScreenTitle.text = getString(com.brijby77.R.string.edit_note_title)
            loadNoteData(noteId)
        } else {
            binding.tvScreenTitle.text = getString(com.brijby77.R.string.new_note_title)
        }

        binding.btnSaveNote.setOnClickListener {
            saveNote()
        }

        binding.btnCancelNote.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadNoteData(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val note = viewModel.getNoteById(id)
            if (note != null) {
                createdAt = note.createdAt
                binding.etNoteTitle.setText(note.title)
                binding.etNoteContent.setText(note.content)
                binding.cbPinNote.isChecked = note.isPinned
            }
        }
    }

    private fun saveNote() {
        val title = binding.etNoteTitle.text?.toString() ?: ""
        val content = binding.etNoteContent.text?.toString() ?: ""
        val isPinned = binding.cbPinNote.isChecked

        if (title.isBlank() && content.isBlank()) {
            Toast.makeText(requireContext(), "कृपया शीर्षक या सामग्री दर्ज करें (Please enter title or content)", Toast.LENGTH_SHORT).show()
            return
        }

        if (noteId == -1L) {
            viewModel.insertNote(title, content, isPinned) {
                Toast.makeText(requireContext(), "सूचना सहेजी गई (Note saved)", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        } else {
            viewModel.updateNote(noteId, title, content, isPinned, createdAt) {
                Toast.makeText(requireContext(), "सूचना अपडेट की गई (Note updated)", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

