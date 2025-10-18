package com.costura.pro.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.R
import com.costura.pro.data.local.entity.MachineEntity
import com.costura.pro.data.local.entity.MachineStatus

class MachinesAdapter(
    private val machines: List<MachineEntity>,
    private val onMachineAction: (MachineEntity, String) -> Unit
) : RecyclerView.Adapter<MachinesAdapter.MachineViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MachineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_machine, parent, false)
        return MachineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MachineViewHolder, position: Int) {
        val machine = machines[position]
        holder.bind(machine)
    }

    override fun getItemCount(): Int = machines.size

    inner class MachineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMachineName: TextView = itemView.findViewById(R.id.tvMachineName)
        private val tvMachineNumber: TextView = itemView.findViewById(R.id.tvMachineNumber)
        private val tvMachineType: TextView = itemView.findViewById(R.id.tvMachineType)
        private val tvMachineDescription: TextView = itemView.findViewById(R.id.tvMachineDescription)
        private val tvMachineStatus: TextView = itemView.findViewById(R.id.tvMachineStatus)

        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        private val btnMaintenance: Button = itemView.findViewById(R.id.btnMaintenance)

        fun bind(machine: MachineEntity) {
            tvMachineName.text = machine.name
            tvMachineNumber.text = "Nº: ${machine.machineNumber}"
            tvMachineType.text = "Tipo: ${machine.type}"
            tvMachineDescription.text = machine.description

            // Configurar estado
            when (machine.status) {
                MachineStatus.OPERATIONAL -> {
                    tvMachineStatus.text = "OPERATIVA"
                    tvMachineStatus.setBackgroundResource(R.drawable.bg_status_active)
                    btnMaintenance.text = "PROBLEMAS"
                    btnMaintenance.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.warning_orange))
                }
                MachineStatus.MAINTENANCE -> {
                    tvMachineStatus.text = "MANTENIMIENTO"
                    tvMachineStatus.setBackgroundResource(R.drawable.bg_status_late)
                    btnMaintenance.text = "REPARADO"
                    btnMaintenance.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.success_green))
                }
                MachineStatus.BROKEN -> {
                    tvMachineStatus.text = "AVERIADA"
                    tvMachineStatus.setBackgroundResource(R.drawable.bg_status_absent)
                    btnMaintenance.text = "REPARADO"
                    btnMaintenance.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.success_green))
                }
            }

            // Configurar botones
            btnEdit.setOnClickListener {
                onMachineAction(machine, "edit")
            }

            btnDelete.setOnClickListener {
                onMachineAction(machine, "delete")
            }

            btnMaintenance.setOnClickListener {
                onMachineAction(machine, "maintenance")
            }
        }
    }
}