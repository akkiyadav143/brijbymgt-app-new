package com.brijby77.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijby77.data.entity.NoteEntity
import com.brijby77.databinding.ItemNoteCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(
    private val onNoteClick: (NoteEntity) -> Unit,
    private val onPinClick: (NoteEntity) -> Unit,
    private val onDeleteClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NoteAdapter.NoteViewHolder>(DiffCallback) {

    inner class NoteViewHolder(val binding: ItemNoteCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(note: NoteEntity) {
            binding.tvNoteTitle.text = note.title.ifBlank { "Untitled Note" }
            binding.tvNoteContent.text = note.content

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.tvNoteDate.text = sdf.format(Date(note.updatedAt))

            binding.ivPinIndicator.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            binding.btnPinNote.setImageResource(
                if (note.isPinned) android.R.drawable.star_on else android.R.drawable.star_off
            )

            binding.root.setOnClickListener { onNoteClick(note) }
            binding.btnPinNote.setOnClickListener { onPinClick(note) }
            binding.btnDeleteNote.setOnClickListener { onDeleteClick(note) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem == newItem
        }
    }
}

