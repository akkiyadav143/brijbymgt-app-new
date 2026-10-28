package com.brijby77.ui.addstudent

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.brijby77.data.entity.HeightWeightHistoryEntity
import com.brijby77.databinding.ItemHeightWeightRowBinding

class HeightWeightAdapter(
    private val items: MutableList<HeightWeightHistoryEntity>,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.Adapter<HeightWeightAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHeightWeightRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHeightWeightRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvRowNumber.text = "${position + 1}."

        // Remove previous listeners to avoid recursion
        holder.binding.etDatePeriod.text = null
        holder.binding.etHeight.text = null
        holder.binding.etWeight.text = null

        holder.binding.etDatePeriod.setText(item.datePeriod)
        holder.binding.etHeight.setText(item.height)
        holder.binding.etWeight.setText(item.weight)

        holder.binding.etDatePeriod.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    items[pos] = items[pos].copy(datePeriod = s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        holder.binding.etHeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    items[pos] = items[pos].copy(height = s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        holder.binding.etWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    items[pos] = items[pos].copy(weight = s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        holder.binding.btnDeleteRow.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDeleteClicked(pos)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}

