package com.example.hotelbooking.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hotelbooking.data.Hotel
import com.example.hotelbooking.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HotelAdapter(
    private val hotels: MutableList<Hotel>,
    private val onHotelClick: (Hotel) -> Unit
) : RecyclerView.Adapter<HotelAdapter.HotelViewHolder>() {
    val database = FirebaseDatabase.getInstance("https://ocp-resv-default-rtdb.europe-west1.firebasedatabase.app/")

    inner class HotelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hotelImage: ImageView = itemView.findViewById(R.id.hotelImage)
        val hotelName: TextView = itemView.findViewById(R.id.hotelName)
        val hotelLocation: TextView = itemView.findViewById(R.id.hotelLocation)
        val hotelPrice: TextView = itemView.findViewById(R.id.hotelPrice)

        fun bind(hotel: Hotel) {
            hotelName.text = hotel.name
            hotelLocation.text = hotel.location
            hotelPrice.text = "Loading price..."
            Glide.with(itemView.context).load(hotel.imageUrl).into(hotelImage)
            itemView.setOnClickListener { onHotelClick(hotel) }
            loadCheapestRoomPrice(hotel.id, hotelPrice)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hotel_card, parent, false)
        return HotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        holder.bind(hotels[position])
    }

    override fun getItemCount(): Int = hotels.size

    fun updateHotels(newHotels: List<Hotel>) {
        hotels.clear()
        hotels.addAll(newHotels)
        notifyDataSetChanged()
    }

    private fun loadCheapestRoomPrice(hotelId: String, priceView: TextView) {
        database.reference.child("hotels").child(hotelId).child("rooms")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var cheapestPrice = Long.MAX_VALUE
                    for (roomSnapshot in snapshot.children) {
                        try {
                            val roomPriceLong = roomSnapshot.child("price").getValue(Long::class.java)
                            roomPriceLong?.let {
                                if (it < cheapestPrice) {
                                    cheapestPrice = it
                                }
                            }
                        } catch (e: Exception) {
                            val roomPriceString = roomSnapshot.child("price").getValue(String::class.java)
                            roomPriceString?.toLongOrNull()?.let {
                                if (it < cheapestPrice) {
                                    cheapestPrice = it
                                }
                            }
                        }
                    }
                    if (cheapestPrice != Long.MAX_VALUE) {
                        priceView.text = "Cheapest room: MAD ${cheapestPrice.toDouble()}"
                    } else {
                        priceView.text = "Price not available"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    priceView.text = "Price not available"
                }
            })
    }
}