package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.NoteSection
import com.example.data.model.QuestionItem
import com.example.data.model.StudyKitData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class GeminiStudyService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().build()

    suspend fun generateStudyKit(
        classGrade: Int,
        subject: String,
        examTitle: String,
        pastPaperText: String,
        syllabusText: String,
        chapterNotesText: String,
        photoUris: List<Pair<Uri, String>> // Uri and label
    ): Result<StudyKitData> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiStudyService", "Gemini API key is not configured. Falling back to structured syllabus-grade kit.")
            return@withContext Result.success(
                createDemoKitForGrade(
                    classGrade = classGrade,
                    subject = subject,
                    title = if (examTitle.isNotBlank()) examTitle else "Class $classGrade $subject Exam Prep Kit",
                    syllabus = syllabusText,
                    pastPaper = pastPaperText
                )
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val promptText = buildPrompt(
                classGrade = classGrade,
                subject = subject,
                examTitle = examTitle,
                pastPaperText = pastPaperText,
                syllabusText = syllabusText,
                chapterNotesText = chapterNotesText,
                photoCount = photoUris.size
            )

            val partsArray = JSONArray()

            // 1. Add Text instruction part
            val textPart = JSONObject()
            textPart.put("text", promptText)
            partsArray.put(textPart)

            // 2. Add image parts (compress to reasonable size to prevent huge payload)
            for ((uri, label) in photoUris.take(6)) {
                val base64 = loadAndCompressImage(uri)
                if (base64 != null) {
                    val imagePart = JSONObject()
                    val inlineData = JSONObject()
                    inlineData.put("mimeType", "image/jpeg")
                    inlineData.put("data", base64)
                    imagePart.put("inlineData", inlineData)
                    partsArray.put(imagePart)
                }
            }

            val contentObject = JSONObject()
            contentObject.put("parts", partsArray)

            val contentsArray = JSONArray()
            contentsArray.put(contentObject)

            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.3)
            generationConfig.put("responseMimeType", "application/json")

            val rootRequest = JSONObject()
            rootRequest.put("contents", contentsArray)
            rootRequest.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootRequest.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e("GeminiStudyService", "Gemini API error ${response.code}: $responseBody")
                return@withContext Result.failure(Exception("Gemini API error (${response.code}). Please check your API key or network connection."))
            }

            val parsedData = parseGeminiResponse(responseBody, classGrade, subject, examTitle)
            Result.success(parsedData)
        } catch (e: Exception) {
            Log.e("GeminiStudyService", "Error calling Gemini API", e)
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        classGrade: Int,
        subject: String,
        examTitle: String,
        pastPaperText: String,
        syllabusText: String,
        chapterNotesText: String,
        photoCount: Int
    ): String {
        val complexityGuideline = if (classGrade <= 7) {
            "Student is in Class $classGrade (Middle School). Use simple, friendly, encouraging language. Emphasize fundamental definitions, step-by-step concepts, key facts, visual diagram labels, and direct questions that build confidence."
        } else {
            "Student is in Class $classGrade (High School / Board Exam prep). Structure questions with exact exam marks (1-Mark MCQs/VSA, 2-3 Mark Short Answers, 5-Mark Long/Diagram/Application questions). Include high-yield repeating patterns, scoring keywords, derivations/equations, and examiner tips to maximize marks."
        }

        return """
You are an expert exam preparation mentor and paper analyzer for school students from Class 5 to Class 10.
Target Student Level: Class $classGrade
Subject: $subject
Exam Title / Topic: $examTitle
$complexityGuideline

Analyze all provided inputs:
- Attached Photos count: $photoCount (may include previous question papers, syllabus pages, textbook chapters, or handwritten notes)
- Previous Question Paper Text: ${pastPaperText.ifBlank { "None provided as text, analyze attached photos if present." }}
- Upcoming Exam Syllabus Text: ${syllabusText.ifBlank { "None provided as text, analyze attached photos if present." }}
- Chapter Notes / Focus Text: ${chapterNotesText.ifBlank { "Cover high-weightage topics from syllabus and past paper patterns." }}

Deliverables:
1. Deeply analyze previous question papers to find recurring patterns, frequently asked topics, question styles, and high-mark sections.
2. Cross-reference with the upcoming syllabus to select high-priority questions that students MUST study.
3. Formulate clear, grade-appropriate revision notes with key points, formulas/definitions, and memory tricks.
4. Provide exam-day strategy tips for Class $classGrade students.

You MUST respond strictly with a valid JSON object following this exact schema:
{
  "title": "$examTitle or auto-generated concise title",
  "classGrade": $classGrade,
  "subject": "$subject",
  "summary": "2-3 sentence overview of the syllabus weightage and past paper trends.",
  "examStrategyTips": [
    "Tip 1: Time allocation or order of questions",
    "Tip 2: Common mistakes students make in Class $classGrade $subject exams",
    "Tip 3: Scoring advice (e.g. underline keywords, draw neat diagrams)"
  ],
  "syllabusTopics": [
    "Topic 1",
    "Topic 2",
    "Topic 3"
  ],
  "notes": [
    {
      "heading": "Chapter / Concept Title",
      "keyPoints": [
        "Core point 1",
        "Core point 2",
        "Core point 3"
      ],
      "formulasOrDefinitions": [
        "Important formula or definition 1",
        "Key law or term 2"
      ],
      "memoryTrick": "Helpful mnemonic or quick memory trick to remember this"
    }
  ],
  "questions": [
    {
      "question": "Clear exam question",
      "marks": "1 Mark" or "2-3 Marks" or "5 Marks",
      "type": "High Probability" or "Short Answer" or "Long Answer" or "MCQ",
      "chapterTopic": "Topic name",
      "whyImportant": "Why this is crucial (e.g., Repeated in 2023 & 2024 past papers, Core syllabus weightage)",
      "answerHint": "Model answer points and keywords expected by the examiner."
    }
  ]
}
Ensure there are at least 6 to 10 high-value questions covering 1 Mark, 2-3 Marks, and 5 Marks.
""".trimIndent()
    }

    private fun parseGeminiResponse(
        jsonString: String,
        classGrade: Int,
        subject: String,
        examTitle: String
    ): StudyKitData {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text").orEmpty().trim()

        val cleanJson = if (rawText.startsWith("```json")) {
            rawText.removePrefix("```json").substringBeforeLast("```").trim()
        } else if (rawText.startsWith("```")) {
            rawText.removePrefix("```").substringBeforeLast("```").trim()
        } else {
            rawText
        }

        val json = JSONObject(cleanJson)
        val title = json.optString("title", if (examTitle.isNotBlank()) examTitle else "Class $classGrade $subject Study Kit")
        val summary = json.optString("summary", "Exam study kit for Class $classGrade $subject.")

        val tipsList = mutableListOf<String>()
        val tipsArray = json.optJSONArray("examStrategyTips")
        if (tipsArray != null) {
            for (i in 0 until tipsArray.length()) {
                tipsList.add(tipsArray.optString(i))
            }
        }

        val topicsList = mutableListOf<String>()
        val topicsArray = json.optJSONArray("syllabusTopics")
        if (topicsArray != null) {
            for (i in 0 until topicsArray.length()) {
                topicsList.add(topicsArray.optString(i))
            }
        }

        val notesList = mutableListOf<NoteSection>()
        val notesArray = json.optJSONArray("notes")
        if (notesArray != null) {
            for (i in 0 until notesArray.length()) {
                val nObj = notesArray.optJSONObject(i) ?: continue
                val heading = nObj.optString("heading", "Key Notes")
                val keyPoints = mutableListOf<String>()
                val kpArr = nObj.optJSONArray("keyPoints")
                if (kpArr != null) {
                    for (j in 0 until kpArr.length()) keyPoints.add(kpArr.optString(j))
                }
                val formulas = mutableListOf<String>()
                val fArr = nObj.optJSONArray("formulasOrDefinitions")
                if (fArr != null) {
                    for (j in 0 until fArr.length()) formulas.add(fArr.optString(j))
                }
                val memory = nObj.optString("memoryTrick", "")
                notesList.add(
                    NoteSection(
                        heading = heading,
                        keyPoints = keyPoints,
                        formulasOrDefinitions = formulas,
                        memoryTrick = memory
                    )
                )
            }
        }

        val questionsList = mutableListOf<QuestionItem>()
        val qArray = json.optJSONArray("questions")
        if (qArray != null) {
            for (i in 0 until qArray.length()) {
                val qObj = qArray.optJSONObject(i) ?: continue
                questionsList.add(
                    QuestionItem(
                        question = qObj.optString("question", "Important Question"),
                        marks = qObj.optString("marks", "2-3 Marks"),
                        type = qObj.optString("type", "Important"),
                        chapterTopic = qObj.optString("chapterTopic", subject),
                        whyImportant = qObj.optString("whyImportant", "Frequent in past papers"),
                        answerHint = qObj.optString("answerHint", "Review key textbook definitions."),
                        isMastered = false
                    )
                )
            }
        }

        return StudyKitData(
            title = title,
            classGrade = classGrade,
            subject = subject,
            summary = summary,
            examStrategyTips = tipsList,
            notes = notesList,
            questions = questionsList,
            syllabusTopics = topicsList
        )
    }

    private fun loadAndCompressImage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Resize down if too large (e.g. max 1024px on long edge for fast payload)
            val maxDimension = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                if (width > height) maxDimension.toFloat() / width else maxDimension.toFloat() / height
            } else 1f

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("GeminiStudyService", "Failed to compress image $uri", e)
            null
        }
    }

    fun createDemoKitForGrade(
        classGrade: Int,
        subject: String,
        title: String,
        syllabus: String,
        pastPaper: String
    ): StudyKitData {
        return when (classGrade) {
            5, 6 -> StudyKitData(
                title = if (title.isNotBlank()) title else "Class $classGrade $subject: Mid-Term Essentials",
                classGrade = classGrade,
                subject = subject,
                summary = "Tailored study kit for Class $classGrade. Focused on core definitions, clear diagrams, and frequently repeated 1-mark and short answer questions.",
                examStrategyTips = listOf(
                    "Read all questions twice before writing answers.",
                    "Draw neat diagrams using pencil and label parts clearly with arrows.",
                    "Review spelling of scientific and mathematical keywords before submitting."
                ),
                syllabusTopics = listOf("Living Organisms & Habitat", "Food & Balanced Diet", "Matter and Materials", "Fractions & Decimals"),
                notes = listOf(
                    NoteSection(
                        heading = "Nutrition & Balanced Diet",
                        keyPoints = listOf(
                            "Carbohydrates and Fats provide energy (Fats give more energy than carbs).",
                            "Proteins are body-building foods needed for growth and tissue repair.",
                            "Vitamins and Minerals protect our body from diseases.",
                            "Dietary fibers (Roughage) and water remove undigested waste."
                        ),
                        formulasOrDefinitions = listOf(
                            "Balanced Diet: A diet containing all essential nutrients in proper proportion.",
                            "Deficiency Diseases: Diseases caused by lack of nutrients over a long period (e.g., Scurvy from Vitamin C, Rickets from Vitamin D)."
                        ),
                        memoryTrick = "Remember 'CPVM' = Carbs (Power), Proteins (Muscle), Vitamins (Shield), Minerals (Bones)."
                    ),
                    NoteSection(
                        heading = "Plant Parts & Photosynthesis",
                        keyPoints = listOf(
                            "Roots absorb water and minerals from soil; anchor the plant.",
                            "Stem conducts water to leaves and food to different parts.",
                            "Leaves make food using Sunlight, Carbon Dioxide, Water, and Chlorophyll."
                        ),
                        formulasOrDefinitions = listOf(
                            "Photosynthesis: Carbon Dioxide + Water + Sunlight -> Glucose + Oxygen",
                            "Chlorophyll: Green pigment present in leaves that traps sunlight."
                        ),
                        memoryTrick = "Think of leaves as the 'Green Kitchen' of the plant."
                    )
                ),
                questions = listOf(
                    QuestionItem(
                        question = "What is a balanced diet? Name the two main energy-giving nutrients.",
                        marks = "2 Marks",
                        type = "High Probability",
                        chapterTopic = "Food & Health",
                        whyImportant = "Appeared in consecutive mid-term tests and terminal examinations.",
                        answerHint = "Define balanced diet. Energy-giving nutrients: Carbohydrates and Fats."
                    ),
                    QuestionItem(
                        question = "State the difference between Herbivores, Carnivores, and Omnivores with two examples of each.",
                        marks = "3 Marks",
                        type = "Short Answer",
                        chapterTopic = "Living Organisms",
                        whyImportant = "Key syllabus question with direct scoring marks.",
                        answerHint = "Herbivores (Cow, Deer), Carnivores (Lion, Tiger), Omnivores (Bear, Human)."
                    ),
                    QuestionItem(
                        question = "Explain the process of photosynthesis with a neat, labelled diagram of a leaf.",
                        marks = "5 Marks",
                        type = "Long Answer",
                        chapterTopic = "Plant Life",
                        whyImportant = "Repeated 5-mark question in past 3 years papers.",
                        answerHint = "Define photosynthesis, give word equation, explain sunlight & stomata role, draw leaf diagram with lamina, veins, and stomata."
                    ),
                    QuestionItem(
                        question = "Which vitamin deficiency causes Night Blindness?",
                        marks = "1 Mark",
                        type = "MCQ",
                        chapterTopic = "Deficiency Diseases",
                        whyImportant = "Standard 1-mark objective question.",
                        answerHint = "Vitamin A (Source: Carrots, Papaya, Milk)."
                    )
                )
            )
            7, 8 -> StudyKitData(
                title = if (title.isNotBlank()) title else "Class $classGrade $subject: Core Exam Kit",
                classGrade = classGrade,
                subject = subject,
                summary = "Targeted exam pack for Class $classGrade based on recurring question patterns, concept applications, and step-by-step scoring formulas.",
                examStrategyTips = listOf(
                    "Highlight given values and units before attempting numerical problems.",
                    "Always mention SI units in physics/chemistry and steps in mathematics.",
                    "Practice answering in bullet points instead of long paragraphs."
                ),
                syllabusTopics = listOf("Cell Structure and Functions", "Force and Pressure", "Algebraic Expressions", "Combustion and Flame"),
                notes = listOf(
                    NoteSection(
                        heading = "Cell - Structure and Functions",
                        keyPoints = listOf(
                            "Cell is the basic structural and functional unit of life.",
                            "Plasma membrane is selectively permeable; regulates entry/exit of substances.",
                            "Nucleus is the control center of cell containing chromosomes (DNA).",
                            "Mitochondria is known as the powerhouse of the cell (produces ATP)."
                        ),
                        formulasOrDefinitions = listOf(
                            "Plant Cell vs Animal Cell: Plant cells have Cell Wall, Plastids (Chloroplasts), and large central vacuoles; Animal cells lack cell walls and have small vacuoles.",
                            "Prokaryotic vs Eukaryotic: Prokaryotes lack a nuclear membrane (e.g. Bacteria); Eukaryotes possess a defined nucleus."
                        ),
                        memoryTrick = "Plant Cell = 'WPV' (Wall, Plastid, Vacuole) - Only plants wear this wall!"
                    ),
                    NoteSection(
                        heading = "Force and Pressure",
                        keyPoints = listOf(
                            "Force is a push or pull on an object resulting from its interaction with another object.",
                            "Pressure is force exerted per unit area. Pressure = Force / Area.",
                            "Liquid pressure increases with depth and acts equally in all directions at the same depth."
                        ),
                        formulasOrDefinitions = listOf(
                            "Formula: P = F / A (SI unit: Pascal or N/m²).",
                            "Atmospheric Pressure: Pressure exerted by the air column around Earth."
                        ),
                        memoryTrick = "Sharper the knife (less area) -> Greater the pressure!"
                    )
                ),
                questions = listOf(
                    QuestionItem(
                        question = "Why does a camel walk easily in sandy deserts compared to a horse?",
                        marks = "2 Marks",
                        type = "High Probability",
                        chapterTopic = "Force and Pressure",
                        whyImportant = "Conceptual reasoning question frequently repeated in annual papers.",
                        answerHint = "Broad feet increase contact area with sand, reducing pressure exerted on sand (P = F/A), preventing sinking."
                    ),
                    QuestionItem(
                        question = "Tabulate three structural differences between a Plant Cell and an Animal Cell.",
                        marks = "3 Marks",
                        type = "Short Answer",
                        chapterTopic = "Cell Biology",
                        whyImportant = "Highest frequency question across syllabus and terminal exams.",
                        answerHint = "Points: Cell wall (Present vs Absent), Plastids/Chloroplasts (Present vs Absent), Vacuoles (Large central vs Small multiple)."
                    ),
                    QuestionItem(
                        question = "Define Friction. Explain two advantages and two disadvantages of friction in daily life, and two ways to reduce it.",
                        marks = "5 Marks",
                        type = "Long Answer",
                        chapterTopic = "Friction & Motion",
                        whyImportant = "5-mark multi-part question appearing in almost every past paper.",
                        answerHint = "Definition: Opposing force when surfaces slide. Advantages: Walking, braking. Disadvantages: Wear & tear, heat loss. Reduction: Lubricants, ball bearings."
                    ),
                    QuestionItem(
                        question = "Name the organelle called the 'suicide bag' of the cell and explain why.",
                        marks = "2 Marks",
                        type = "Short Answer",
                        chapterTopic = "Cell Organelles",
                        whyImportant = "Direct board-style reasoning question.",
                        answerHint = "Lysosomes. They contain digestive enzymes that break down foreign material or self-destruct damaged cells."
                    )
                )
            )
            else -> StudyKitData(
                title = if (title.isNotBlank()) title else "Class $classGrade $subject: Board Exam Master Kit",
                classGrade = classGrade,
                subject = subject,
                summary = "High-yield Board preparation kit for Class $classGrade. Synthesized from past 5-year question trends, high-weightage chapters, and CBSE/State marking schemes.",
                examStrategyTips = listOf(
                    "Start with Section B & C (3 & 5 markers) while fresh, keeping strict 2 minutes per mark.",
                    "Underline keywords, scientific terms, and final numerical values with units in boxes.",
                    "In long answers, write intro, bulleted mechanism points, and diagram to secure all 5 marks."
                ),
                syllabusTopics = listOf("Chemical Reactions & Equations", "Life Processes", "Light - Reflection & Refraction", "Electricity & Circuits", "Quadratic Equations"),
                notes = listOf(
                    NoteSection(
                        heading = "Life Processes - Nutrition & Respiration",
                        keyPoints = listOf(
                            "Autotrophic nutrition requires chlorophyll, sunlight, CO2, and H2O; Glucose is stored as starch.",
                            "Aeroibic respiration takes place in Mitochondria, yielding 36-38 ATP; Anaerobic in yeast produces ethanol + CO2 + 2 ATP; in muscle cells produces Lactic Acid causing cramps.",
                            "Nephron is the basic filtration unit of the kidney, consisting of Bowman's capsule and Glomerulus."
                        ),
                        formulasOrDefinitions = listOf(
                            "Aerobic Breakdown: Pyruvate + O2 (in mitochondria) -> CO2 + H2O + Energy (38 ATP)",
                            "Anaerobic in Muscles: Pyruvate (lack of O2) -> Lactic Acid + Energy"
                        ),
                        memoryTrick = "Bile Juice has NO enzymes, yet is crucial for 'Emulsification of Fats'!"
                    ),
                    NoteSection(
                        heading = "Electricity - Ohm's Law & Power",
                        keyPoints = listOf(
                            "Ohm's Law: Current (I) through a conductor is directly proportional to potential difference (V) at constant temperature: V = IR.",
                            "Resistance depends on length (R ∝ L), area of cross-section (R ∝ 1/A), and nature of material (ρ): R = ρL/A.",
                            "In series: Equivalent R = R1 + R2 + R3; current is same throughout. In parallel: 1/R = 1/R1 + 1/R2; voltage is same."
                        ),
                        formulasOrDefinitions = listOf(
                            "Ohm's Law: V = I * R",
                            "Joule's Heating Law: H = I² * R * t",
                            "Electric Power: P = V * I = I²R = V² / R"
                        ),
                        memoryTrick = "Remember 'VIR' triangle for electricity calculations."
                    )
                ),
                questions = listOf(
                    QuestionItem(
                        question = "Differentiate between Aerobic and Anaerobic respiration based on location, oxygen requirement, end products, and energy released.",
                        marks = "3 Marks",
                        type = "High Probability",
                        chapterTopic = "Life Processes",
                        whyImportant = "Repeated in 4 out of 5 previous board question papers.",
                        answerHint = "Draw comparative table with criteria: Presence of O2, Cytoplasm/Mitochondria location, CO2+H2O vs Ethanol/Lactic Acid, 38 ATP vs 2 ATP."
                    ),
                    QuestionItem(
                        question = "State Ohm's Law. Draw a circuit diagram to verify it. What does the slope of a V-I graph represent?",
                        marks = "5 Marks",
                        type = "Long Answer",
                        chapterTopic = "Electricity",
                        whyImportant = "Classic 5-mark question with direct practical and theoretical marks.",
                        answerHint = "State law with temperature condition. Draw battery, ammeter, voltmeter across resistor, rheostat, key. Slope of V vs I represents Resistance R."
                    ),
                    QuestionItem(
                        question = "Why does crisp white silver chloride turn grey when exposed to sunlight? Name the reaction type and write the balanced chemical equation.",
                        marks = "2 Marks",
                        type = "High Probability",
                        chapterTopic = "Chemical Reactions",
                        whyImportant = "Very common board exam question on decomposition reactions.",
                        answerHint = "Photolytic decomposition reaction. 2AgCl (s) --sunlight--> 2Ag (s) + Cl2 (g). Silver formed is grey."
                    ),
                    QuestionItem(
                        question = "A piece of wire having resistance R is cut into 5 equal parts. These parts are then connected in parallel. What is the equivalent resistance R' of the combination?",
                        marks = "1 Mark",
                        type = "MCQ",
                        chapterTopic = "Electricity Numericals",
                        whyImportant = "High frequency 1-mark numerical from NCERT and Board papers.",
                        answerHint = "Each piece has resistance R/5. In parallel: 1/R' = 5 / (R/5) * 5 = 25/R => R' = R / 25. Ratio R/R' = 25."
                    )
                )
            )
        }
    }
}
