package com.example.rosabetaniapedidos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.chip.Chip

class PedidoAdapter(
    private var pedidos: List<Pedido>,
    private val onItemClick: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    fun updateData(newPedidos: List<Pedido>) {
        pedidos = newPedidos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidos[position]
        holder.bind(pedido)
        holder.itemView.setOnClickListener { onItemClick(pedido) }
    }

    override fun getItemCount(): Int = pedidos.size

    inner class PedidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreTrabajo = view.findViewById<TextView>(R.id.tvNombreTrabajo)
        val tvCliente       = view.findViewById<TextView>(R.id.tvCliente)
        val tvDetalles      = view.findViewById<TextView>(R.id.tvDetalles)
        val tvPrecio        = view.findViewById<TextView>(R.id.tvPrecio)
        val chipEstado      = view.findViewById<Chip>(R.id.chipEstado)
        val ivImagen        = view.findViewById<ImageView>(R.id.ivPedidoImagen)

        fun bind(pedido: Pedido) {
            tvNombreTrabajo.text = pedido.nombre_trabajo
            tvCliente.text = pedido.cliente_email
            tvDetalles.text = "${pedido.cantidad} unid. | ${pedido.tipo_trabajo} | ${pedido.largo_cm}×${pedido.ancho_cm} cm | ${pedido.material}"
            
            if (pedido.precio_cotizado != null) {
                tvPrecio.text = "Precio: $${pedido.precio_cotizado}"
            } else {
                tvPrecio.text = "Precio por definir"
            }
            tvPrecio.visibility = View.VISIBLE

            chipEstado.text = pedido.estado

            // Colores brand del sistema de diseño, sin dependencias de android.R.color
            val ctx = chipEstado.context
            when (pedido.estado) {
                "Pendiente" -> {
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FF8F00"))
                    chipEstado.setTextColor(Color.WHITE)
                }
                "Cotizado" -> {
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#0277BD"))
                    chipEstado.setTextColor(Color.WHITE)
                }
                "Confirmado" -> {
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.rb_green_dark)
                        )
                    chipEstado.setTextColor(Color.WHITE)
                }
                "Rechazado" -> {
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#C62828"))
                    chipEstado.setTextColor(Color.WHITE)
                }
                else -> {
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.rb_nav_inactive)
                        )
                    chipEstado.setTextColor(Color.WHITE)
                }
            }

            // Imagen del diseño
            val url = pedido.imagen_url
            if (!url.isNullOrBlank()) {
                ivImagen.visibility = View.VISIBLE
                Glide.with(ivImagen.context)
                    .load(url)
                    .apply(RequestOptions().transform(RoundedCorners(16)))
                    .into(ivImagen)
            } else {
                ivImagen.visibility = View.GONE
            }
        }
    }
}
