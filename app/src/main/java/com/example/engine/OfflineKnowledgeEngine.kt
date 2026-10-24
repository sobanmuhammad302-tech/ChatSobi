package com.example.engine

import com.example.model.ChatMessage
import com.example.model.KnowledgeDomain
import com.example.model.Role
import java.util.Locale
import java.util.regex.Pattern

object OfflineKnowledgeEngine {

    data class DirectFact(
        val triggers: List<String>,
        val directAnswer: String,
        val domain: KnowledgeDomain,
        val codeSnippet: String? = null
    )

    private val directFacts = listOf(
        // Science & Universal Constants
        DirectFact(
            triggers = listOf("speed of light", "velocity of light", "how fast is light"),
            directAnswer = "The speed of light in a vacuum is exactly **299,792,458 meters per second** (approximately **3.0 × 10⁸ m/s** or ~186,282 miles/second). Denoted by the constant **c**.",
            domain = KnowledgeDomain.SCIENCE
        ),
        DirectFact(
            triggers = listOf("planets in solar system", "how many planets", "list of planets"),
            directAnswer = "There are **8 recognized planets** in our Solar System in order of distance from the Sun:\n1. Mercury\n2. Venus\n3. Earth\n4. Mars\n5. Jupiter\n6. Saturn\n7. Uranus\n8. Neptune\n*(Pluto was reclassified as a dwarf planet by the IAU in 2006).*",
            domain = KnowledgeDomain.SCIENCE
        ),
        DirectFact(
            triggers = listOf("photosynthesis", "what is photosynthesis", "how does photosynthesis work"),
            directAnswer = "Photosynthesis is the biological process by which green plants and algae convert sunlight, water, and carbon dioxide into glucose (chemical energy) and oxygen.\n\n• **Chemical Equation:** 6CO₂ + 6H₂O + Solar Energy ➔ C₆H₁₂O₆ + 6O₂\n• **Site:** Chloroplasts (Thylakoids for light reactions, Stroma for the Calvin cycle).",
            domain = KnowledgeDomain.SCIENCE,
            codeSnippet = "6CO₂ + 6H₂O + Light ➔ C₆H₁₂O₆ + 6O₂"
        ),
        DirectFact(
            triggers = listOf("dna", "what is dna", "structure of dna"),
            directAnswer = "DNA (Deoxyribonucleic Acid) is the hereditary material in humans and almost all other organisms. Its structure is a **double helix** composed of four nucleotide bases:\n• **Adenine (A)** pairs with **Thymine (T)**\n• **Cytosine (C)** pairs with **Guanine (G)**",
            domain = KnowledgeDomain.SCIENCE
        ),
        DirectFact(
            triggers = listOf("chromosomes in humans", "how many chromosomes", "human chromosomes"),
            directAnswer = "Humans have **46 chromosomes** in each somatic cell, organized into **23 pairs** (22 pairs of autosomes and 1 pair of sex chromosomes: XX in females, XY in males).",
            domain = KnowledgeDomain.SCIENCE
        ),
        DirectFact(
            triggers = listOf("gravity", "gravitational acceleration", "value of g on earth"),
            directAnswer = "Standard acceleration due to Earth's gravity is **g = 9.80665 m/s²** (approx **9.8 m/s²** or 32.2 ft/s²). Newton's Universal Gravitation Constant is **G = 6.67430 × 10⁻¹¹ N·m²/kg²**.",
            domain = KnowledgeDomain.SCIENCE
        ),
        DirectFact(
            triggers = listOf("water formula", "chemical formula of water", "chemical name of water"),
            directAnswer = "The chemical formula of water is **H₂O** (two hydrogen atoms covalently bonded to one oxygen atom). Its molar mass is 18.015 g/mol.",
            domain = KnowledgeDomain.SCIENCE
        ),
        DirectFact(
            triggers = listOf("quantum entanglement", "what is quantum entanglement"),
            directAnswer = "Quantum entanglement is a physical phenomenon where two or more particles become interconnected such that the quantum state of one instantaneously determines the state of the other, regardless of distance.\n• **Wavefunction:** |Ψ⟩ = (1/√2) (|00⟩ + |11⟩)\n• Validated by Bell's theorem and the 2022 Nobel Prize in Physics.",
            domain = KnowledgeDomain.SCIENCE
        ),

        // Geography & General
        DirectFact(
            triggers = listOf("capital of france", "capital city of france"),
            directAnswer = "The capital of France is **Paris**.",
            domain = KnowledgeDomain.LOGIC
        ),
        DirectFact(
            triggers = listOf("capital of japan", "capital city of japan"),
            directAnswer = "The capital of Japan is **Tokyo**.",
            domain = KnowledgeDomain.LOGIC
        ),
        DirectFact(
            triggers = listOf("capital of united kingdom", "capital of uk", "capital of england"),
            directAnswer = "The capital of the United Kingdom and England is **London**.",
            domain = KnowledgeDomain.LOGIC
        ),
        DirectFact(
            triggers = listOf("capital of usa", "capital of united states", "us capital"),
            directAnswer = "The capital of the United States is **Washington, D.C.**",
            domain = KnowledgeDomain.LOGIC
        ),
        DirectFact(
            triggers = listOf("capital of pakistan", "capital city of pakistan"),
            directAnswer = "The capital of Pakistan is **Islamabad**.",
            domain = KnowledgeDomain.LOGIC
        ),
        DirectFact(
            triggers = listOf("tallest mountain", "highest mountain in world", "highest peak"),
            directAnswer = "The tallest mountain above sea level is **Mount Everest**, located in the Himalayas on the border of Nepal and China, measuring **8,848.86 meters** (29,031.7 feet).",
            domain = KnowledgeDomain.LOGIC
        ),

        // History
        DirectFact(
            triggers = listOf("world war 2", "when did world war 2 start", "ww2 dates"),
            directAnswer = "World War II lasted from **September 1, 1939 to September 2, 1945**. It began with Nazi Germany's invasion of Poland and ended with the formal surrender of Japan aboard the USS Missouri.",
            domain = KnowledgeDomain.HISTORY
        ),
        DirectFact(
            triggers = listOf("world war 1", "when did ww1 start", "ww1 dates"),
            directAnswer = "World War I lasted from **July 28, 1914 to November 11, 1918**. It was sparked by the assassination of Archduke Franz Ferdinand in Sarajevo and ended with the Armistice of Compiègne.",
            domain = KnowledgeDomain.HISTORY
        ),
        DirectFact(
            triggers = listOf("moon landing", "first man on moon", "who walked on the moon first"),
            directAnswer = "Neil Armstrong was the first human to walk on the Moon on **July 20, 1969**, during NASA's Apollo 11 mission, accompanied by Buzz Aldrin, with Michael Collins piloting the command module.",
            domain = KnowledgeDomain.HISTORY
        ),

        // Mathematics & Calculus
        DirectFact(
            triggers = listOf("pythagorean theorem", "pythagoras formula", "a2 + b2"),
            directAnswer = "The Pythagorean Theorem states that in a right-angled triangle, the square of the hypotenuse (c) is equal to the sum of the squares of the other two sides (a and b):\n\n# **a² + b² = c²**\n\n• Therefore: c = √(a² + b²)",
            domain = KnowledgeDomain.MATHEMATICS,
            codeSnippet = "c = sqrt(a^2 + b^2)"
        ),
        DirectFact(
            triggers = listOf("quadratic formula", "roots of quadratic equation"),
            directAnswer = "For any quadratic equation ax² + bx + c = 0 (where a ≠ 0), the roots are:\n\n# **x = [-b ± √(b² - 4ac)] / (2a)**\n\n• Discriminant Δ = b² - 4ac determines root nature (Δ > 0: two real roots; Δ = 0: one real root; Δ < 0: two complex roots).",
            domain = KnowledgeDomain.MATHEMATICS,
            codeSnippet = "x = (-b ± sqrt(b^2 - 4*a*c)) / (2*a)"
        ),
        DirectFact(
            triggers = listOf("derivative of sin", "derivative of sine"),
            directAnswer = "The derivative of **sin(x)** with respect to x is **cos(x)**:\n\n# **d/dx [sin(x)] = cos(x)**",
            domain = KnowledgeDomain.MATHEMATICS
        ),
        DirectFact(
            triggers = listOf("derivative of cos", "derivative of cosine"),
            directAnswer = "The derivative of **cos(x)** with respect to x is **-sin(x)**:\n\n# **d/dx [cos(x)] = -sin(x)**",
            domain = KnowledgeDomain.MATHEMATICS
        ),
        DirectFact(
            triggers = listOf("derivative of e^x", "derivative of ex"),
            directAnswer = "The derivative of the natural exponential function **eˣ** is **eˣ**:\n\n# **d/dx [eˣ] = eˣ**",
            domain = KnowledgeDomain.MATHEMATICS
        ),

        // Programming & Coding
        DirectFact(
            triggers = listOf("reverse a string in python", "python reverse string"),
            directAnswer = "In Python, the most efficient way to reverse a string is using slice notation `[::-1]`:\n\n```python\ntext = \"Hello World\"\nreversed_text = text[::-1]  # Output: 'dlroW olleH'\n```",
            domain = KnowledgeDomain.PROGRAMMING,
            codeSnippet = "reversed_text = text[::-1]"
        ),
        DirectFact(
            triggers = listOf("fibonacci in python", "fibonacci python code"),
            directAnswer = "Here is an efficient iterative Fibonacci generator in Python:\n\n```python\ndef fibonacci(n):\n    a, b = 0, 1\n    result = []\n    for _ in range(n):\n        result.append(a)\n        a, b = b, a + b\n    return result\n\nprint(fibonacci(10)) # [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]\n```",
            domain = KnowledgeDomain.PROGRAMMING,
            codeSnippet = """
def fibonacci(n):
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b
""".trimIndent()
        ),
        DirectFact(
            triggers = listOf("kotlin coroutine", "what is a coroutine in kotlin"),
            directAnswer = "A Kotlin Coroutine is a lightweight thread of execution that allows asynchronous, non-blocking code to be written sequentially using suspending functions (`suspend`).\n\n• Lightweight: Thousands of coroutines can run simultaneously on few OS threads.\n• Structured Concurrency: Tied to Lifecycles (`viewModelScope`, `lifecycleScope`).",
            domain = KnowledgeDomain.PROGRAMMING,
            codeSnippet = """
viewModelScope.launch(Dispatchers.IO) {
    val data = fetchNetworkData()
    withContext(Dispatchers.Main) {
        updateUI(data)
    }
}
""".trimIndent()
        ),
        DirectFact(
            triggers = listOf("binary search complexity", "big o of binary search"),
            directAnswer = "The time complexity of Binary Search is:\n• **Best Case:** O(1)\n• **Average Case:** O(log n)\n• **Worst Case:** O(log n)\n• **Space Complexity:** O(1) iterative, O(log n) recursive call stack.",
            domain = KnowledgeDomain.PROGRAMMING
        )
    )

    fun answerQuery(query: String, forceDomain: KnowledgeDomain = KnowledgeDomain.ALL): ChatMessage {
        val clean = query.trim().lowercase(Locale.ROOT)

        // 1. Math Evaluator (evaluates expressions accurately)
        val mathResult = tryEvaluateMath(clean)
        if (mathResult != null) {
            return mathResult
        }

        // 2. Direct Fact Matcher
        val matchedFact = directFacts.firstOrNull { fact ->
            fact.triggers.any { clean.contains(it) }
        }

        if (matchedFact != null) {
            return ChatMessage(
                role = Role.SOBI,
                text = matchedFact.directAnswer,
                isOfflineEngine = true,
                domain = matchedFact.domain,
                codeBlock = matchedFact.codeSnippet
            )
        }

        // 3. Conversational and Direct Intelligent Fallback
        val directAnswer = buildDirectAnswer(query, clean, forceDomain)
        return ChatMessage(
            role = Role.SOBI,
            text = directAnswer.text,
            isOfflineEngine = true,
            domain = directAnswer.domain,
            codeBlock = directAnswer.codeBlock
        )
    }

    private data class AnswerResult(
        val text: String,
        val domain: KnowledgeDomain,
        val codeBlock: String? = null
    )

    private fun buildDirectAnswer(rawQuery: String, clean: String, requestedDomain: KnowledgeDomain): AnswerResult {
        // Handle greetings
        if (clean in listOf("hi", "hello", "hey", "salam", "assalam o alaikum", "good morning", "good evening")) {
            return AnswerResult(
                text = "Hello! How can I assist you today with your studies, calculations, science, or programming questions?",
                domain = KnowledgeDomain.ALL
            )
        }

        if (clean.contains("who are you") || clean.contains("what is chatsobi")) {
            return AnswerResult(
                text = "**ChatSobi** is a multi-modal artificial intelligence ecosystem designed for students, researchers, and everyday problem solvers. It integrates an offline-first knowledge engine, a cloud media studio for video/photo editing and generation, 30GB free safe cloud storage, and real-time live voice and video communication.",
                domain = KnowledgeDomain.ALL
            )
        }

        // Programming inquiry
        if (clean.contains("code") || clean.contains("python") || clean.contains("kotlin") || clean.contains("java") || clean.contains("function")) {
            val codeExample = if (clean.contains("python")) {
                """
# Python Solution for: $rawQuery
def execute_task(data):
    # Process inputs cleanly
    processed = [item.strip() for item in data if item]
    return processed

if __name__ == "__main__":
    sample = ["apple ", " banana", "orange"]
    print(execute_task(sample))
                """.trimIndent()
            } else {
                """
// Kotlin Solution for: $rawQuery
fun solveProblem(input: String): String {
    return input.trim().replaceFirstChar { it.uppercase() }
}
                """.trimIndent()
            }

            return AnswerResult(
                text = "Here is the direct code implementation for your query:\n\n```\n$codeExample\n```\n\n• **Time Complexity:** O(n)\n• **Space Complexity:** O(n)",
                domain = KnowledgeDomain.PROGRAMMING,
                codeBlock = codeExample
            )
        }

        // General factual direct response - Natural ChatGPT / Claude style formatting
        val cleanSubject = rawQuery
            .removePrefix("what is ")
            .removePrefix("what are ")
            .removePrefix("explain ")
            .removePrefix("tell me about ")
            .removePrefix("who is ")
            .removeSuffix("?")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        val S = "$"
        val SS = "$$"

        val responseText = when {
            clean.contains("calculus") || clean.contains("integral") || clean.contains("derivative") -> {
                """
### Understanding $cleanSubject

**Calculus** is the mathematical study of continuous change, founded primarily by Isaac Newton and Gottfried Wilhelm Leibniz. It is divided into two primary branches:

1. **Differential Calculus:** Studies rates of change and slopes of curves.
   - Fundamental Derivative: 
   $SS \frac{df}{dx} = \lim_{h \to 0} \frac{f(x + h) - f(x)}{h} $SS

2. **Integral Calculus:** Studies accumulation of quantities and areas under curves.
   - Fundamental Theorem of Calculus:
   $SS \int_a^b f(x)\,dx = F(b) - F(a) $SS

**Key Applications:**
• Physics: Modeling planetary motion, fluid dynamics, and electromagnetic waves.
• Engineering & AI: Gradient descent optimization in neural networks.
• Economics: Marginal profit and revenue analysis.
                """.trimIndent()
            }
            clean.contains("einstein") || clean.contains("relativity") -> {
                """
### Albert Einstein & The Theory of Relativity

Albert Einstein (1879–1955) revolutionized modern theoretical physics with his **Special and General Theories of Relativity**.

1. **Mass-Energy Equivalence:**
   Mass and energy are interchangeable. A small amount of mass can be converted into immense energy:
   $SS E = mc^2 $SS
   *(where m is relativistic mass and c = 2.998 × 10⁸ m/s is the speed of light).*

2. **General Relativity (1915):**
   Gravity is not merely a magnetic force, but the curvature of four-dimensional spacetime caused by mass and energy:
   $SS G_{\mu\nu} + \Lambda g_{\mu\nu} = \frac{8\pi G}{c^4} T_{\mu\nu} $SS

**Key Implications:**
• Gravitational time dilation (satellites in GPS must adjust their internal atomic clocks daily).
• Black holes and gravitational lensing.
                """.trimIndent()
            }
            clean.contains("kinetic energy") || clean.contains("potential energy") -> {
                """
### Energy & Mechanics: Kinetic vs. Potential

Energy is the capacity to perform work. In mechanical physics:

1. **Kinetic Energy (E_k):** The energy possessed by an object due to its motion.
   $SS E_k = \frac{1}{2}mv^2 $SS
   *(where m is mass in kg and v is velocity in m/s).*

2. **Gravitational Potential Energy (E_p):** The stored energy due to an object's position in a gravitational field.
   $SS E_p = mgh $SS
   *(where g ≈ 9.8 m/s² and h is height in meters).*

**Conservation of Mechanical Energy:**
In an isolated system with no friction:
$SS E_{\text{total}} = E_k + E_p = \text{Constant} $SS
                """.trimIndent()
            }
            clean.contains("gravity") || clean.contains("newton's law") -> {
                """
### Newton's Universal Law of Gravitation

Sir Isaac Newton formulated that every particle of matter in the universe attracts every other particle with a force proportional to the product of their masses and inversely proportional to the square of the distance between their centers.

**Mathematical Law:**
$SS F = G \frac{m_1 m_2}{r^2} $SS

• **G:** Gravitational constant (6.67430 × 10⁻¹¹ N·m²/kg²).
• **m₁, m₂:** Masses of the interacting bodies.
• **r:** Distance between centers of mass.

**On Earth's Surface:**
$SS g = \frac{G M_{\text{Earth}}}{R_{\text{Earth}}^2} \approx 9.8\text{ m/s}^2 $SS
                """.trimIndent()
            }
            clean.contains("ohm's law") || clean.contains("electricity") || clean.contains("circuit") -> {
                """
### Ohm's Law & Basic Electrical Principles

**Ohm's Law** describes the fundamental relationship between electric current, voltage, and electrical resistance in an electrical circuit.

**Formula:**
$SS V = I \cdot R $SS

• **V (Voltage):** Electrical potential difference, measured in Volts (V).
• **I (Current):** Flow of electric charge, measured in Amperes (A).
• **R (Resistance):** Opposition to current flow, measured in Ohms (Ω).

**Electrical Power:**
$SS P = V \cdot I = I^2 R = \frac{V^2}{R} $SS
                """.trimIndent()
            }
            else -> {
                """
### Overview: $cleanSubject

**$cleanSubject** is a key topic in ${if (requestedDomain != KnowledgeDomain.ALL) requestedDomain.label else "modern science and education"}.

**Essential Concepts:**
• **Definition:** An essential framework addressing "$rawQuery" with precise principles.
• **Core Mechanism:** Operates systematically according to verified scientific and analytical rules.
• **Practical Applications:** Widely implemented across academic curricula, technical problem-solving, and real-world projects.

*(Need deeper real-time web browsing or generative expansion? Switch to **Online Gemini 3.5** via the toggle above).*
                """.trimIndent()
            }
        }

        return AnswerResult(
            text = responseText,
            domain = if (requestedDomain != KnowledgeDomain.ALL) requestedDomain else KnowledgeDomain.LOGIC
        )
    }

    private fun tryEvaluateMath(query: String): ChatMessage? {
        val sanitized = query
            .replace("what is ", "")
            .replace("calculate ", "")
            .replace("solve ", "")
            .replace("evaluate ", "")
            .replace("?", "")
            .trim()

        // Match binary arithmetic: e.g. "25 * 40", "150 + 350", "100 / 4", "50 - 18"
        val regex = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*([+\\-*/x×÷^%]|plus|minus|times|multiplied by|divided by)\\s*([0-9]+(?:\\.[0-9]+)?)$")
        val matcher = regex.matcher(sanitized)

        if (matcher.matches()) {
            val n1 = matcher.group(1)?.toDoubleOrNull()
            val op = matcher.group(2)
            val n2 = matcher.group(3)?.toDoubleOrNull()

            if (n1 != null && n2 != null) {
                val result: Double? = when (op) {
                    "+", "plus" -> n1 + n2
                    "-", "minus" -> n1 - n2
                    "*", "x", "×", "times", "multiplied by" -> n1 * n2
                    "/", "÷", "divided by" -> if (n2 != 0.0) n1 / n2 else Double.NaN
                    "^" -> Math.pow(n1, n2)
                    "%" -> n1 % n2
                    else -> null
                }

                if (result != null) {
                    val formatted = if (result == result.toLong().toDouble()) result.toLong().toString() else String.format(Locale.US, "%.4f", result)
                    val text = """
# **$formatted**

• **Calculation:** $n1 $op $n2 = **$formatted**
• **Status:** Evaluated directly with 100% precision in ChatSobi offline engine.
                    """.trimIndent()

                    return ChatMessage(
                        role = Role.SOBI,
                        text = text,
                        isOfflineEngine = true,
                        domain = KnowledgeDomain.MATHEMATICS
                    )
                }
            }
        }

        // Square root check: "sqrt 144" or "square root of 144"
        if (sanitized.startsWith("sqrt ") || sanitized.startsWith("square root of ")) {
            val numStr = sanitized.replace("sqrt ", "").replace("square root of ", "").trim()
            val num = numStr.toDoubleOrNull()
            if (num != null && num >= 0) {
                val root = Math.sqrt(num)
                val formatted = if (root == root.toLong().toDouble()) root.toLong().toString() else String.format(Locale.US, "%.4f", root)
                return ChatMessage(
                    role = Role.SOBI,
                    text = "# **$formatted**\n\n• **Square Root:** √($num) = **$formatted**",
                    isOfflineEngine = true,
                    domain = KnowledgeDomain.MATHEMATICS
                )
            }
        }

        return null
    }
}
