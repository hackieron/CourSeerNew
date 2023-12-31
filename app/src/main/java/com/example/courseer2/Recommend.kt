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

class Recommend : Fragment() {
    companion object {
        fun newInstance(strandBased: Boolean): Recommend {
            val fragment = Recommend()
            val args = Bundle()
            args.putBoolean("strandBased", strandBased)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: RProgramAdapter

    private lateinit var programs: List<RProgram>
    private lateinit var allPrograms: List<RProgram>
    private var filteredPrograms: List<RProgram> = emptyList()

    private var basisValues = mutableListOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recommend, container, false)
        searchView = rootView.findViewById(R.id.searchView)
        recyclerView = rootView.findViewById(R.id.programRecyclerView)

        val csvData = readCSVFileFromAssets(requireContext())

        programs = parseCSVData(csvData)
        val dataBaseHandler = DataBaseHandler(requireContext())
        basisValues = dataBaseHandler.getAllBasisValues() as MutableList<String>

        // Sort all programs based on the scores in descending order
        allPrograms = programs.sortedByDescending { program ->
            calculateProgramScore(program)
        }

        // Filter programs with a score of 4 or higher
        val localFilteredPrograms = allPrograms.filter { program ->
            calculateProgramScore(program) >= 3 &&
                    basisValues.firstOrNull { value ->
                        program.strand.contains(value, ignoreCase = true)
                    } != null
        }

        adapter = RProgramAdapter(localFilteredPrograms as MutableList<RProgram>, object : RProgramAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                // Handle item click if needed
            }
        })

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = filterPrograms(newText)
                val sortedList = filteredList.filter { program ->
                    calculateProgramScore(program) >= 3 &&
                            basisValues.firstOrNull { value ->
                                program.strand.contains(value, ignoreCase = true)
                            } != null
                }.sortedByDescending { program ->
                    calculateProgramScore(program)
                }
                adapter.updatePrograms(sortedList)
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

    private fun parseCSVData(csvData: String): List<RProgram> {
        val programs = mutableListOf<RProgram>()
        val lines = csvData.split('|')
        for (line in lines) {
            val columns = line.split(";")
            if (columns.size >= 7) {
                val title = columns[0]
                val category = columns[1]
                val shortDescription = columns[2]
                val fullDescription = columns[3]
                val subcar = columns[4]
                val strand = columns[5]
                val keywords = columns[6]
                val program = RProgram(
                    title,
                    category,
                    shortDescription,
                    fullDescription,
                    subcar,
                    strand,
                    keywords
                )
                programs.add(program)
            }
        }
        return programs
    }

    private fun calculateProgramScore(program: RProgram): Int {
        return basisValues.sumOf { value ->
            val regex = "\\b${Regex.escape(value)}\\b"
            val occurrences = Regex(regex, RegexOption.IGNORE_CASE).findAll(
                "${program.title} ${program.category} ${program.shortDescription} ${program.fullDescription} ${program.subcar} ${program.strand} ${program.keywords} "
            ).count()
            occurrences
        }
    }


    private fun filterPrograms(query: String?): List<RProgram> {
        // Filter programs based on title or fullDescription containing the query and the first value of basisValues
        return programs.filter { program ->
            program.title.contains(query.orEmpty(), ignoreCase = true) ||
                    program.fullDescription.contains(query.orEmpty(), ignoreCase = true) ||
                    basisValues.firstOrNull { value ->
                        program.fullDescription.contains(value, ignoreCase = true)
                    } != null
        }
    }
}
data class RProgram(
    val title: String,
    val category:String,
    val shortDescription: String,
    val fullDescription: String,
    val subcar: String,
    val strand: String,
    val keywords: String
)
