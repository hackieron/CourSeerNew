package com.example.courseer2

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class Recommend2 : Fragment() {
    companion object {
        fun newInstance(strandBased: Boolean): Recommend2 {
            val fragment = Recommend2()
            val args = Bundle()
            args.putBoolean("nonStrandBased", strandBased)
            fragment.arguments = args
            return fragment
        }
    }
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter2: RProgramAdapter2
    private lateinit var programs1: List<Rprograms>
    private lateinit var allPrograms: List<Rprograms>
    private var filteredPrograms: List<Rprograms> = emptyList()
    private var basisValues = mutableListOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recommend2, container, false)
        searchView = rootView.findViewById(R.id.searchView)
        recyclerView = rootView.findViewById(R.id.programRecyclerView2)

        val csvData = readCSVFileFromAssets(requireContext())

        programs1 = parseCSVData(csvData)
        val dataBaseHandler = DataBaseHandler(requireContext())
        basisValues = dataBaseHandler.getAllBasisValues() as MutableList<String>

        // Sort all programs based on the scores in descending order
        allPrograms = programs1.sortedByDescending { program1 ->
            calculateProgramScore(program1)
        }

        // Filter programs with a score of 4 or higher
        val localFilteredPrograms = allPrograms.filter { program1 ->
            calculateProgramScore(program1) >= 2
        }

        adapter2 = RProgramAdapter2(localFilteredPrograms as MutableList<Rprograms>, object : RProgramAdapter2.OnItemClickListener {
            override fun onItemClick(position: Int) {
                // Handle item click if needed
            }
        })

        recyclerView.adapter = adapter2
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = filterPrograms(newText)
                val sortedList = filteredList.filter { program ->
                    calculateProgramScore(program) >= 2
                }.sortedByDescending { program ->
                    calculateProgramScore(program)
                }
                adapter2.updatePrograms(sortedList)
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

    private fun parseCSVData(csvData: String): List<Rprograms> {
        val programs = mutableListOf<Rprograms>()
        val lines = csvData.split('|')
        for (line in lines) {
            val columns = line.split(";")
            if (columns.size >= 7) {
                val title = columns[0]
                val category = columns[1]
                val shortDescription = columns[2]
                val fullDescription = columns[3]
                val subcar = columns[4]
                val keywords = columns[6]
                val program1 = Rprograms(title, category, shortDescription, fullDescription, subcar, keywords )
                programs.add(program1)
            }
        }
        return programs
    }

    private fun calculateProgramScore(program1: Rprograms): Int {
        return basisValues.sumOf { value ->
            val regex = "\\b${Regex.escape(value)}\\b"
            val occurrences2 = Regex(regex, RegexOption.IGNORE_CASE).findAll(
                "${program1.title2} ${program1.category} ${program1.shortDescription} ${program1.fullDescription} ${program1.subcar} ${program1.keywords} "
            ).count()
            occurrences2
        }
    }

    private fun filterPrograms(query: String?): List<Rprograms> {
        // Filter programs based on title or fullDescription containing the query
        return programs1.filter { program1 ->
            program1.title2.contains(query.orEmpty(), ignoreCase = true) ||
                    program1.fullDescription.contains(query.orEmpty(), ignoreCase = true)||
                    program1.shortDescription.contains(query.orEmpty(), ignoreCase = true)||
                    program1.subcar.contains(query.orEmpty(), ignoreCase = true)||
                    program1.category.contains(query.orEmpty(), ignoreCase = true)||
                    program1.keywords.contains(query.orEmpty(), ignoreCase = true)
        }
    }
}
data class Rprograms(
    val title2: String,
    val category:String,
    val shortDescription: String,
    val fullDescription: String,
    val subcar: String,
    val keywords: String
)

