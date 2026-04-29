package com.example.rosabetaniapedidos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class ProduccionAdapter(
    private var pedidos: List<Pedido>,
    private val onCambiarEstado: (Pedido) -> Unit
) : RecyclerView.Adapter<ProduccionAdapter.ProduccionViewHolder>() {

    fun updateData(nuevos: List<Pedido>) {
        pedidos = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProduccionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido_produccion, parent, false)
        return ProduccionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProduccionViewHolder, position: Int) {
        holder.bind(pedidos[position])
    }

    override fun getItemCount(): Int = pedidos.size

    inner class ProduccionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre   = view.findViewById<TextView>(R.id.tvProdNombre)
        val tvCliente  = view.findViewById<TextView>(R.id.tvProdCliente)
        val tvDetalles = view.findViewById<TextView>(R.id.tvProdDetalles)
        val tvPrecio   = view.findViewById<TextView>(R.id.tvProdPrecio)
        val chipEstado = view.findViewById<Chip>(R.id.chipProdEstado)
        val stripe     = view.findViewById<View>(R.id.stripeProduccion)
        val btnCambiar = view.findViewById<MaterialButton>(R.id.btnCambiarEstado)

        fun bind(pedido: Pedido) {
            tvNombre.text   = pedido.nombre_trabajo
            tvCliente.text  = pedido.cliente_email
            tvDetalles.text = "${pedido.cantidad} unid. | ${pedido.tipo_trabajo} | " +
                              "${pedido.largo_cm}×${pedido.ancho_cm} cm | ${pedido.material}"

            tvPrecio.text = if (pedido.precio_cotizado != null)
                "Precio: $${pedido.precio_cotizado}"
            else
                "Precio no asignado"

            // El estado "Confirmado" se muestra como "Pendiente" en producción
            val estadoMostrado = if (pedido.estado == "Confirmado") "Pendiente" else pedido.estado
            chipEstado.text = estadoMostrado

            // Colores de chip y franja según estado
            when (pedido.estado) {
                "Confirmado" -> {                         // Pendiente de producción
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FF8F00"))
                    chipEstado.setTextColor(Color.WHITE)
                    stripe.setBackgroundColor(Color.parseColor("#FF8F00"))
                }
                "En proceso" -> {
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#0277BD"))
                    chipEstado.setTextColor(Color.WHITE)
                    stripe.setBackgroundColor(Color.parseColor("#0277BD"))
                }
                "Terminado" -> {
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.context, R.color.rb_green_dark)
                        )
                    chipEstado.setTextColor(Color.WHITE)
                    stripe.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.rb_green_dark)
                    )
                }
                else -> {
                    chipEstado.chipBackgroundColor =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.context, R.color.rb_nav_inactive)
                        )
                    chipEstado.setTextColor(Color.WHITE)
                }
            }

            // Solo se puede cambiar estado si NO está Terminado
            if (pedido.estado == "Terminado") {
                btnCambiar.visibility = View.GONE
            } else {
                btnCambiar.visibility = View.VISIBLE
                btnCambiar.setOnClickListener { onCambiarEstado(pedido) }
            }
        }
    }
}
