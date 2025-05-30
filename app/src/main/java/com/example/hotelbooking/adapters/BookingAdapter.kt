package com.example.hotelbooking.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hotelbooking.data.Booking
import com.example.hotelbooking.BookingAction
import com.example.hotelbooking.databinding.BookingItemBinding

class BookingsAdapter(private val onAction: (Booking, BookingAction) -> Unit) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {
    private var bookings: List<Booking> = emptyList()

    fun submitList(newBookings: List<Booking>) {
        bookings = newBookings.sortedBy { it.departDate }
        notifyDataSetChanged()
    }

    fun removeBooking(booking: Booking) {
        val newList = bookings.toMutableList().apply { remove(booking) }
        submitList(newList)
    }

    fun updateBooking(updatedBooking: Booking) {
        val newList = bookings.toMutableList().apply {
            val index = indexOfFirst { it.bookingId == updatedBooking.bookingId }
            if (index != -1) set(index, updatedBooking)
        }
        submitList(newList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = BookingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding, onAction)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    class BookingViewHolder(
        private val binding: BookingItemBinding,
        private val onAction: (Booking, BookingAction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: Booking) {
            binding.hotelNameTextView.text = booking.hotelName
            binding.roomNameTextView.text = booking.roomName
            binding.bookingDatesTextView.text = "${booking.departDate} - ${booking.returnDate}"

            binding.statusButton.setOnClickListener { onAction(booking, BookingAction.ChangeStatus) }
            binding.cancelBookingButton.setOnClickListener { onAction(booking, BookingAction.Cancel) }
            binding.rateAndCommentButton.setOnClickListener { onAction(booking, BookingAction.Rate) }

            if (booking.isActive) {
                binding.cancelBookingButton.visibility = View.VISIBLE
                binding.rateAndCommentButton.visibility = View.GONE
            } else {
                binding.cancelBookingButton.visibility = View.GONE
                binding.rateAndCommentButton.visibility = View.VISIBLE
            }
        }
    }
}