package com.classmanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.classmanager.app.data.Payment
import com.classmanager.app.data.fmtINR
import com.classmanager.app.databinding.ItemPaymentBinding

/** Newest-first payment rows; tapping one shares its receipt PDF. */
class PaymentAdapter(private val onClick: (Payment) -> Unit) :
    RecyclerView.Adapter<PaymentAdapter.Holder>() {
    private var items: List<Payment> = emptyList()

    fun submit(items: List<Payment>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun latest(): Payment? = items.firstOrNull()

    class Holder(val binding: ItemPaymentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemPaymentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val payment = items[position]
        holder.binding.receiptNo.text = payment.receiptNo
        holder.binding.amount.text = fmtINR(payment.amount)
        holder.binding.meta.text = "${payment.method}  |  ${payment.date}"
        holder.binding.root.setOnClickListener { onClick(payment) }
    }

    override fun getItemCount(): Int = items.size
}
