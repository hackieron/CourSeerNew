package com.example.courseer2

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


class Programs : Fragment() {
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryAdapter
    private lateinit var programs: List<Program>
    private lateinit var allPrograms: List<Program>
    private var filteredPrograms: List<Program> = emptyList()
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_programs, container, false)
        searchView = rootView.findViewById(R.id.searchView)
        recyclerView = rootView.findViewById(R.id.programRecyclerView)


        val csvData = readCSVFileFromAssets(requireContext())
        programs = parseCSVData(csvData)
        allPrograms = parseCSVData(csvData)

        filteredPrograms = allPrograms
        adapter = CategoryAdapter(
            groupProgramsByCategory(filteredPrograms),
            object : ProgramAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    // Handle item click if needed
                }
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filteredPrograms = filterPrograms(newText)
                adapter.updatePrograms(groupProgramsByCategory(filteredPrograms))
                return true
            }
        })

        return rootView
    }


    private fun readCSVFileFromAssets(context: Context): String {
        val fileName = "Programs.csv"
        val inputStream = context.assets.open(fileName)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?

        try {
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
                stringBuilder.append('\n')
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return stringBuilder.toString()
    }

    private fun parseCSVData(csvData: String): List<Program> {
        val programs = mutableListOf<Program>()
        val lines = csvData.split('|')
        for (line in lines) {
            val columns = line.split(";")
            if (columns.size >= 7) {
                val title = columns[0]
                val category = columns[1]
                val shortDescription = columns[2]
                val fullDescription = columns[3]
                val subcar = columns[4]
                val program = Program(title, category, shortDescription, fullDescription, subcar)
                programs.add(program)
            }
        }
        return programs
    }

    private fun groupProgramsByCategory(programs: List<Program>): Map<String, List<Program>> {
        return programs.groupBy { it.category }
    }

    private fun filterPrograms(query: String?): List<Program> {
        return programs.filter { program ->
            program.title.contains(query.orEmpty(), ignoreCase = true) ||
                    program.fullDescription.contains(query.orEmpty(), ignoreCase = true) || program.category.contains(query.orEmpty(), ignoreCase = true) || program.subcar.contains(query.orEmpty(), ignoreCase = true) || program.shortDescription.contains(query.orEmpty(), ignoreCase = true )
        }
    }
}

data class Program(
    val title: String,
    val category: String,
    val shortDescription: String,
    val fullDescription: String,
    val subcar:String
)


class CategoryAdapter(
    private var programsByCategory: Map<String, List<Program>>,
    private val itemClickListener: ProgramAdapter.OnItemClickListener
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        val programRecyclerView: RecyclerView = itemView.findViewById(R.id.programRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = programsByCategory.keys.toList()[position]
        holder.categoryName.text = category
        val programAdapter =
            ProgramAdapter(programsByCategory[category] ?: emptyList(), itemClickListener)
        holder.programRecyclerView.adapter = programAdapter
        holder.programRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
    }

    override fun getItemCount(): Int {
        return programsByCategory.size
    }

    fun updatePrograms(programsByCategory: Map<String, List<Program>>) {
        this.programsByCategory = programsByCategory
        notifyDataSetChanged()
    }
}
