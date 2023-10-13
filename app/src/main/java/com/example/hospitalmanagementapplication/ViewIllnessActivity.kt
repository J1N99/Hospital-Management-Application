package com.example.hospitalmanagementapplication

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hospitalmanagementapplication.databinding.ActivityViewillnessBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.Illness
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class ViewIllnessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewillnessBinding
    private val illnessList = mutableListOf<Illness>()
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewillnessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerView = binding.recyclerView
        val adapter = IllnessAdapter(this, illnessList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        firestore().getAllIllness { allIllness ->
            illnessList.clear()
            illnessList.addAll(allIllness)
            adapter.notifyDataSetChanged()
        }

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        binding.searchBarText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                adapter.filter(query)
            }
        })
    }
}

class IllnessAdapter(private val context: Context, private val illnessList: MutableList<Illness>) :
    RecyclerView.Adapter<IllnessAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameOfIllness: TextView = itemView.findViewById(R.id.nameOfIllness)
        val description: TextView = itemView.findViewById(R.id.description)
        val actionTaken: TextView = itemView.findViewById(R.id.actionTaken)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.illness_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val illness = illnessList[position]
        holder.nameOfIllness.text = illness.illnessName
        holder.description.text = illness.description
        holder.actionTaken.text = illness.actionTaken
    }

    override fun getItemCount(): Int {
        return illnessList.size
    }

    fun addAll(illnesses: List<Illness>) {
        illnessList.addAll(illnesses)
    }

    fun filter(query: String) {
        val filteredList = mutableListOf<Illness>()
        for (item in illnessList) {
            if (item.illnessName.contains(query, ignoreCase = true)) {
                filteredList.add(item)
            }
        }
        illnessList.clear()
        illnessList.addAll(filteredList)
        notifyDataSetChanged()
    }
}



