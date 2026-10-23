package kz.edu.sdu.advisor.config;

import kz.edu.sdu.advisor.model.*;
import kz.edu.sdu.advisor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Seeds the SDU academic structure based on the official
 * Module Handbook of the educational programme Information Systems BSc.
 * (6B06101-Information Systems, SITAM, Kaskelen, 2026).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final FacultyRepository facultyRepository;
    private final MajorRepository majorRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final StudentAccountRepository studentAccountRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        cleanLegacyData();

        Faculty sitam = seedFaculties();
        Major isMajor = seedMajor(sitam, "IS", "Information Systems");

        Map<String, Course> courses = seedCourses();
        seedPrerequisites(courses);
        for (int year = 2022; year <= 2026; year++) {
            seedCurriculum(isMajor, year, courses);
        }
        seedDemoStudent(isMajor, courses);
    }

    private void cleanLegacyData() {
        Faculty sitam = seedFaculties();
        Major isMajor = seedMajor(sitam, "IS", "Information Systems");

        // 1. Reassign any existing students and student accounts pointing to legacy majors to IS
        studentRepository.findAll().forEach(s -> {
            if (s.getMajor() == null || !"IS".equals(s.getMajor().getCode())) {
                s.setMajor(isMajor);
                studentRepository.save(s);
            }
        });
        studentAccountRepository.findAll().forEach(acc -> {
            if (acc.getMajor() == null || !"IS".equals(acc.getMajor().getCode())) {
                acc.setMajor(isMajor);
                studentAccountRepository.save(acc);
            }
        });

        // 2. Safely remove legacy majors and their curriculum courses
        majorRepository.findAll().stream()
                .filter(m -> !"IS".equals(m.getCode()))
                .forEach(m -> {
                    log.info("Purging legacy major: {}", m.getCode());
                    curriculumCourseRepository.deleteByMajor_Id(m.getId());
                    majorRepository.delete(m);
                });

        // 3. Safely remove legacy faculties
        facultyRepository.findAll().stream()
                .filter(f -> !"SITAM".equals(f.getCode()))
                .forEach(f -> {
                    log.info("Purging legacy faculty: {}", f.getCode());
                    facultyRepository.delete(f);
                });
    }

    private Faculty seedFaculties() {
        return facultyRepository.findByCode("SITAM").orElseGet(() -> {
            log.info("Seeding faculty: SITAM");
            Faculty faculty = new Faculty("SITAM", "SITAM");
            return facultyRepository.save(faculty);
        });
    }

    private Major seedMajor(Faculty faculty, String code, String name) {
        return majorRepository.findByCode(code).orElseGet(() -> {
            log.info("Seeding major: {} ({})", code, name);
            Major major = new Major(code, name, faculty, 240);
            return majorRepository.save(major);
        });
    }

    private Map<String, Course> seedCourses() {
        Map<String, Course> map = new HashMap<>();

        List<CourseSeed> catalog = List.of(
                // ── Semester 1 ──
                new CourseSeed("INF106", "Information and Communication Technologies", 5, 2, 1, "CORE",
                        "Foundational concepts in computer science and IT: number systems, hardware/software, logic circuits, compression, cryptography, SQL databases, networking, and AI/ML basics."),
                new CourseSeed("MDE160", "Community Engagement and Value Based Society 1", 0, 1, 1, "CORE",
                        "Academic integrity, personal well-being, ethical behavior, inclusivity, digital literacy, time management, and social responsibility."),
                new CourseSeed("MDE171", "History of Kazakhstan", 5, 2, 1, "CORE",
                        "Major political, social, and cultural transformations in Kazakhstan's history from ancient nomadic civilization and the Golden Horde to modern independence."),
                new CourseSeed("CSS105", "Fundamentals of Programming", 5, 2, 1, "CORE",
                        "Fundamental programming concepts, variables, control structures, functions, arrays and collections, algorithmic problem solving, and complexity basics."),
                new CourseSeed("MAT137", "Mathematics for Information Systems 1", 5, 3, 1, "CORE",
                        "Mathematical foundations for IS: functions and graphs, limits and continuity, differential calculus, applications of derivatives, and optimization problems."),
                new CourseSeed("MAT156", "Discrete Mathematics", 5, 3, 1, "CORE",
                        "Logic and proof techniques, sets and relations, functions, combinatorics, graph theory, trees, and discrete structures for computing."),
                new CourseSeed("MDE190", "Foreign Language 1", 5, 1, 1, "ELECTIVE",
                        "Academic English reading, writing, listening, speaking, critical thinking, academic vocabulary, and structured oral presentations."),
                new CourseSeed("MDE291", "Physical Education 1", 2, 1, 1, "CORE",
                        "Physical culture, health, hygiene, and team sports (basketball, volleyball, football, fitness, chess)."),

                // ── Semester 2 ──
                new CourseSeed("INF114", "Fundamentals of Information Systems", 5, 2, 2, "CORE",
                        "Role of Information Systems in organizations and society, business processes, IS components, IS development lifecycle, ethics, security, cloud and AI trends."),
                new CourseSeed("INF108", "Object-Oriented Programming and Educational Practice", 6, 3, 2, "CORE",
                        "Object-oriented design principles in Java: classes and objects, encapsulation, inheritance, polymorphism, exception handling, and software development project."),
                new CourseSeed("MAT138", "Mathematics for Information Systems 2", 5, 4, 2, "CORE",
                        "Integral calculus, techniques and applications of integration, differential equations, and mathematical modelling for information systems."),
                new CourseSeed("MAT151", "Linear Algebra", 5, 3, 2, "CORE",
                        "Matrices and determinants, systems of linear equations, vector spaces, linear transformations, eigenvalues and eigenvectors."),
                new CourseSeed("MDE170", "Community Engagement and Value-Based Society 2", 0, 1, 2, "CORE",
                        "Abay's Tolyq Adam concept, virtue ethics, emotional intelligence, growth mindset, and community project implementation."),
                new CourseSeed("MDE191", "Foreign Language 2", 5, 2, 2, "ELECTIVE",
                        "Advanced academic English communication, critical reading, analytical essays, research presentations, and debate skills."),
                new CourseSeed("MDE292", "Physical Education 2", 2, 1, 2, "CORE",
                        "Intermediate physical education, sports tactics, endurance, and team sports practice."),

                // ── Semester 3 ──
                new CourseSeed("MAT251", "Probability and Mathematical Statistics", 5, 4, 3, "CORE",
                        "Probability theory, random variables, probability distributions, estimation and hypothesis testing, correlation, regression, and statistical inference."),
                new CourseSeed("CSS215", "Introduction to Algorithms", 5, 4, 3, "CORE",
                        "Algorithm design techniques, recursion, sorting and searching, asymptotic complexity analysis, and fundamental data structures."),
                new CourseSeed("CSS331", "Operating Systems", 5, 4, 3, "CORE",
                        "Operating system architecture, process management, memory management, file systems, and Linux system administration."),
                new CourseSeed("INF313", "Computer Networks 1", 5, 3, 3, "CORE",
                        "CCENT-aligned networking foundations: OSI and TCP/IP models, switching, VLAN/interVLAN, Cisco IOS configuration, IPv4/IPv6 subnetting, routing, DHCP, FHRP, and WLAN."),
                new CourseSeed("CSS118", "AI and Prompt Engineering", 5, 3, 3, "CORE",
                        "AI fundamentals, generative AI models, prompt design techniques, LLM pipelines, and practical AI applications for digital transformation."),
                new CourseSeed("MDE111", "Kazakh Language 1 / Russian Language 1", 5, 1, 3, "ELECTIVE",
                        "Academic and professional communication, grammar, and reading analysis in Kazakh / Russian language (Level 1)."),
                new CourseSeed("MDE293", "Physical Education 3", 2, 1, 3, "CORE",
                        "Applied physical training, athletic conditioning, and competitive team sports."),

                // ── Semester 4 ──
                new CourseSeed("CSS217", "Software Architecture and Design Patterns", 5, 4, 4, "CORE",
                        "Software engineering principles, SDLC models, GoF design patterns, UML modelling, architectural styles, and software quality attributes."),
                new CourseSeed("INF202", "Database Management Systems 1", 5, 3, 4, "CORE",
                        "Relational database concepts, ER modeling, normalization, SQL fundamentals and querying in PostgreSQL, transactions, constraints, and NoSQL overview."),
                new CourseSeed("INF995", "Fundamentals of Web Development", 5, 3, 4, "CORE",
                        "Full-stack web development fundamentals: HTML, CSS, JavaScript, front-end frameworks, REST APIs, server-side development, and database integration."),
                new CourseSeed("INF203", "Information Security", 5, 3, 4, "CORE",
                        "Security principles, cryptography, access control, Linux shell security, digital forensics, incident response frameworks, and threat detection."),
                new CourseSeed("INF211", "Educational Practice 2", 1, 2, 4, "CORE",
                        "Introduction to professional IT environments, observation of organizational processes, and practical project reporting."),
                new CourseSeed("MDE112", "Kazakh Language 2 / Russian Language 2", 5, 2, 4, "ELECTIVE",
                        "Professional and scientific communication, technical terminology, and academic writing in Kazakh / Russian language (Level 2)."),
                new CourseSeed("MDE294", "Physical Education 4", 2, 1, 4, "CORE",
                        "Final semester of compulsory physical education and sports mastery."),

                // ── Semester 5 ──
                new CourseSeed("MDE153", "Module of Social and Political Knowledge (Cultural Studies)", 2, 1, 5, "CORE",
                        "Theories of cultural studies, digital media culture, ethnicity and nation, and cultural heritage of Kazakhstan."),
                new CourseSeed("MDE154", "Module of Social and Political Knowledge (Psychology)", 2, 1, 5, "CORE",
                        "Cognitive processes, perception, memory, learning, emotional intelligence, motivation, personality, and mental health."),
                new CourseSeed("INF381", "Project Management in IT Business", 5, 3, 5, "CORE",
                        "IT project planning, risk management, Agile and traditional methodologies, resource management, and stakeholder communication."),
                new CourseSeed("CSS280", "Industrial Practice 1", 2, 2, 5, "CORE",
                        "Practical work in IT organizations, participation in software projects, and application of academic knowledge in professional settings."),
                new CourseSeed("MDE161", "Economy", 5, 2, 5, "ELECTIVE",
                        "Micro and macroeconomic models, supply and demand, elasticity, market efficiency, taxation, and firm behavior."),
                new CourseSeed("MDE162", "Law", 5, 2, 5, "ELECTIVE",
                        "Constitutional law, civil and criminal law, legal systems, human rights, and labor legislation of the Republic of Kazakhstan."),
                new CourseSeed("MDE163", "Fundamentals of Anti-Corruption Culture", 5, 1, 5, "ELECTIVE",
                        "Anti-corruption legislation, transparency, accountability, ethical decision-making, and civic responsibility."),
                new CourseSeed("MDE164", "Ecology", 5, 1, 5, "ELECTIVE",
                        "Ecosystems, biodiversity, environmental protection, climate change, and sustainable development principles."),
                new CourseSeed("MDE166", "Entrepreneurship", 5, 2, 5, "ELECTIVE",
                        "Value propositions, Business Model Canvas, startup funding, lean validation, and investor pitching."),
                new CourseSeed("MDE289", "Financial Literacy", 5, 2, 5, "ELECTIVE",
                        "Financial statements, time value of money, bonds and stocks valuation, risk and return, capital budgeting, and SME finance."),
                new CourseSeed("INF317", "Data Analysis and Storytelling", 5, 3, 5, "ELECTIVE",
                        "Data preparation, exploratory and statistical analysis in Python (NumPy, Pandas, Seaborn), BI dashboards, and data-driven storytelling."),
                new CourseSeed("INF777", "Digital Marketing Technologies", 5, 2, 5, "ELECTIVE",
                        "Digital business models, marketing analytics, campaign management, and customer behavior analysis."),
                new CourseSeed("INF330", "Information Storage Management and Virtualization", 5, 3, 5, "ELECTIVE",
                        "Modern storage architectures, data center virtualization, virtual machines, data protection, and disaster recovery."),
                new CourseSeed("INF345", "Fundamentals of DevOps", 5, 4, 5, "ELECTIVE",
                        "Continuous integration and deployment (CI/CD), Docker containerization, infrastructure as code, and monitoring."),
                new CourseSeed("INF318", "Introduction to Testing", 5, 3, 5, "ELECTIVE",
                        "Software quality assurance, testing methodologies, test planning, defect classification, and test case design."),
                new CourseSeed("INF328", "3D Modeling and Animation", 5, 3, 5, "ELECTIVE",
                        "Polygonal 3D modeling, materials and texturing, lighting, rendering, rigging, and animation principles."),
                new CourseSeed("INF321", "Machine Learning", 5, 4, 5, "ELECTIVE",
                        "Supervised and unsupervised learning, feature engineering, model evaluation, and applied ML pipelines."),
                new CourseSeed("INF429", "IT Audit and Controls", 5, 3, 5, "ELECTIVE",
                        "IT auditing principles, COBIT/ISACA internal controls, risk assessment, compliance, and IT assurance."),
                new CourseSeed("INF316", "Web Front-End Development", 5, 3, 5, "ELECTIVE",
                        "Modern front-end web engineering, component architectures, state management, REST integration, and web performance."),
                new CourseSeed("CSS216", "Mobile Programming", 5, 3, 5, "ELECTIVE",
                        "Mobile application architecture, Android/iOS development with Jetpack Compose, UI lifecycle, and mobile APIs."),
                new CourseSeed("INF314", "Computer Networks 2", 5, 4, 5, "ELECTIVE",
                        "Enterprise networking, advanced routing and switching, network security, and network automation."),
                new CourseSeed("INF322", "Graphic Design", 5, 2, 5, "ELECTIVE",
                        "Principles of visual communication, typography, color theory, composition, branding, and UI asset design."),
                new CourseSeed("INF888", "Data Engineering", 5, 4, 5, "ELECTIVE",
                        "Data pipelines, ETL processes, data integration, data storage architectures, and big data fundamentals."),
                new CourseSeed("INF228", "UX/UI Design", 5, 2, 5, "ELECTIVE",
                        "User-centered design, UX research, user journeys, wireframing, design systems, and high-usability prototyping."),
                new CourseSeed("INF357", "Introduction to Unity Programming", 5, 3, 5, "ELECTIVE",
                        "Interactive application and game development in Unity: game mechanics, physics, animation, and 3D environments."),
                new CourseSeed("INF319", "Advanced Algorithms", 5, 5, 5, "ELECTIVE",
                        "Dynamic programming, greedy algorithms, advanced graph algorithms, divide-and-conquer, and optimization techniques."),
                new CourseSeed("INF347", "Cyber Security", 5, 4, 5, "ELECTIVE",
                        "Network and application defense, CompTIA Security+ concepts, vulnerability assessment, and threat mitigation."),
                new CourseSeed("INF999", "Signal Processing", 5, 4, 5, "ELECTIVE",
                        "Continuous and discrete-time signals, Fourier transform, digital filter design, feature extraction, and time-frequency analysis."),

                // ── Semester 6 ──
                new CourseSeed("CSS410", "Research Tools and Methods", 5, 3, 6, "CORE",
                        "Research design, scientific methodology, literature review techniques, data collection, and academic writing."),
                new CourseSeed("MDE151", "Module of Social and Political Knowledge (Political Science)", 2, 1, 6, "CORE",
                        "Political systems, state and civil society, governance models, democratization, and policy analysis."),
                new CourseSeed("MDE152", "Module of Social and Political Knowledge (Sociology)", 2, 1, 6, "CORE",
                        "Sociological theory, research methods, social stratification, socialization, and contemporary social processes."),
                new CourseSeed("INF395", "Advanced Projects for Information Systems", 5, 4, 6, "CORE",
                        "Designing, architecting, and implementing deployable enterprise Information Systems in multidisciplinary teams."),
                new CourseSeed("INF324", "Computer Vision", 5, 4, 6, "ELECTIVE",
                        "Image processing, object detection, image classification, OpenCV, and deep vision systems."),
                new CourseSeed("INF305", "Database Management Systems 2", 5, 4, 6, "ELECTIVE",
                        "Advanced SQL, database administration, transactions and concurrency control, recovery, and query optimization."),
                new CourseSeed("INF373", "Web Back-End Development", 5, 4, 6, "ELECTIVE",
                        "Server-side architecture, RESTful microservices, authentication, database integration, and scalable backend engineering."),
                new CourseSeed("INF389", "Information Technology Infrastructure Library (ITIL)", 5, 3, 6, "ELECTIVE",
                        "IT service management practices, ITIL service lifecycle, incident and change management, and continuous service improvement."),
                new CourseSeed("INF234", "Mobile Application Development 2", 5, 4, 6, "ELECTIVE",
                        "Advanced mobile engineering, reactive state management, offline persistence, background services, and production deployment."),
                new CourseSeed("INF390", "Advanced DevOps", 5, 5, 6, "ELECTIVE",
                        "Kubernetes orchestration, distributed platform engineering, automated security pipelines, and high-availability infrastructure."),
                new CourseSeed("INF337", "Deep Learning", 5, 5, 6, "ELECTIVE",
                        "Deep neural networks, CNNs, RNNs, Transformers, representation learning, and modern generative AI architectures."),
                new CourseSeed("INF374", "Business Process Modelling", 5, 3, 6, "ELECTIVE",
                        "Business process analysis, BPMN 2.0 modelling, workflow automation, and process optimization."),
                new CourseSeed("INF392", "Blockchain Technology", 5, 4, 6, "ELECTIVE",
                        "Distributed ledger technologies, consensus mechanisms, cryptographic trust, smart contracts, and decentralized applications."),
                new CourseSeed("INF368", "The Go Programming Language", 5, 4, 6, "ELECTIVE",
                        "Go programming, goroutines and channels, concurrent systems, and high-performance backend services."),
                new CourseSeed("INF998", "MLOps", 5, 4, 6, "ELECTIVE",
                        "Machine learning operationalization, model deployment, CI/CD for AI, model monitoring, and lifecycle management."),
                new CourseSeed("INF332", "Enterprise Architecture", 5, 3, 6, "ELECTIVE",
                        "Enterprise architecture frameworks, business-IT alignment, application and technology architecture, and digital transformation."),
                new CourseSeed("INF376", "Natural Language Processing", 5, 4, 6, "ELECTIVE",
                        "Text preprocessing, language modeling, sentiment analysis, information extraction, and transformer-based NLP."),
                new CourseSeed("INF300", "Product Management", 5, 3, 6, "ELECTIVE",
                        "Product lifecycle management, market and user research, product strategy, roadmapping, and agile product delivery."),
                new CourseSeed("INF378", "Cloud Computing", 5, 4, 6, "ELECTIVE",
                        "Cloud service models (IaaS/PaaS/SaaS), virtualization, containerization, and scalable cloud deployment."),
                new CourseSeed("INF356", "Game Development", 5, 4, 6, "ELECTIVE",
                        "Advanced game engineering, interactive simulations, AI in games, rendering pipelines, and multi-platform publishing."),
                new CourseSeed("INF997", "Fundamentals of 1C Development", 5, 3, 6, "ELECTIVE",
                        "Enterprise resource planning (ERP) systems and business application development on the 1C:Enterprise platform."),
                new CourseSeed("INF377", "Automation in Testing", 5, 4, 6, "ELECTIVE",
                        "Automated software testing frameworks, continuous test integration, UI/API test automation, and quality metrics."),

                // ── Semester 7 ──
                new CourseSeed("MDE172", "Philosophy", 5, 2, 7, "CORE",
                        "Ontology, epistemology, ethics, history of world and Kazakh philosophy, and philosophy of science and AI."),
                new CourseSeed("CSS483", "Industrial Practice 2", 5, 3, 7, "CORE",
                        "Extended industry internship in software engineering, data analytics, or information systems management."),
                new CourseSeed("INF387", "Communication Management", 5, 2, 7, "ELECTIVE",
                        "Stakeholder communication planning, professional reporting, negotiation, conflict resolution, and escalation management."),
                new CourseSeed("INF406", "Data Ethics and Applied Analytics", 5, 3, 7, "ELECTIVE",
                        "Data privacy, confidentiality, algorithmic bias, fairness, transparency, legal compliance, and responsible analytics."),
                new CourseSeed("INF407", "Business Analysis and System Development", 5, 3, 7, "ELECTIVE",
                        "Requirements engineering, stakeholder elicitation, UML/BPMN modeling, SRS documentation, and solution design."),
                new CourseSeed("INF415", "Leadership in IT", 5, 2, 7, "ELECTIVE",
                        "Leadership theories, engineering team management, organizational behavior, and leading digital change."),
                new CourseSeed("CSS314", "Parallel Computing", 5, 5, 7, "ELECTIVE",
                        "Parallel computing architectures, task and data parallelism, concurrency, synchronization, and distributed performance analysis."),
                new CourseSeed("INF412", "IT Asset Management", 5, 3, 7, "ELECTIVE",
                        "IT asset lifecycle, configuration management, licensing, cost optimization, and service governance."),
                new CourseSeed("INF360", "IT Law", 5, 2, 7, "ELECTIVE",
                        "Information technology legislation, personal data protection, intellectual property rights, and cyber law."),
                new CourseSeed("INF365", "Fundamentals of Finance", 5, 3, 7, "ELECTIVE",
                        "Financial management fundamentals, corporate finance, investment evaluation, and financial analytics."),
                new CourseSeed("INF414", "IT Governance, Risk and Compliance", 5, 3, 7, "ELECTIVE",
                        "IT governance frameworks, enterprise risk management, regulatory compliance, and internal control systems."),
                new CourseSeed("INF996", "Advanced Business Intelligence Systems", 5, 4, 7, "ELECTIVE",
                        "BI architecture, enterprise data warehouses, OLAP systems, KPI executive dashboards, and decision support systems."),
                new CourseSeed("INF433", "ICT in Agriculture", 5, 3, 7, "ELECTIVE",
                        "Precision agriculture technologies, GIS/QGIS spatial modeling, IoT telemetry, and smart farming information systems."),
                new CourseSeed("INF410", "Introduction to Building Information Modelling", 5, 3, 7, "ELECTIVE",
                        "BIM fundamentals, parametric 3D modeling in Revit, collaborative virtual environments, and building performance analysis."),

                // ── Semester 8 ──
                new CourseSeed("INF420", "Senior Project", 10, 5, 8, "CORE",
                        "Capstone engineering project: problem identification, requirements analysis, system design, implementation, testing, and defence."),
                new CourseSeed("INF459", "Start Ups", 6, 3, 8, "CORE",
                        "Creating and managing IT startup ventures: ideation, business model development, market validation, and investor pitch."),
                new CourseSeed("CSS486", "Industrial Practice 3", 6, 3, 8, "CORE",
                        "Senior-year professional practice in an industry IT environment."),
                new CourseSeed("CSS495", "Pregraduation Practice", 6, 3, 8, "CORE",
                        "Pregraduation research and system implementation practice preparing for final thesis defence."),
                new CourseSeed("CSS479", "Final Assessment (Diploma Thesis / Project or Comprehensive Examination)", 8, 5, 8, "CORE",
                        "Writing and defending a Diploma Thesis / Diploma Project or passing the state Comprehensive Examination.")
        );

        for (CourseSeed seed : catalog) {
            Course course = courseRepository.findByCode(seed.code()).orElseGet(() -> {
                Course c = new Course();
                c.setCode(seed.code());
                c.setName(seed.name());
                c.setCredits(seed.credits());
                c.setDifficulty(seed.difficulty());
                c.setDescription(seed.description());
                return courseRepository.save(c);
            });
            boolean updated = false;
            if (course.getDifficulty() == null) {
                course.setDifficulty(seed.difficulty());
                updated = true;
            }
            if (course.getDescription() == null) {
                course.setDescription(seed.description());
                updated = true;
            }
            if (updated) {
                course = courseRepository.save(course);
            }
            map.put(seed.code(), course);
        }

        this.seededCatalogList = catalog;
        return map;
    }

    private List<CourseSeed> seededCatalogList = List.of();

    private void seedPrerequisites(Map<String, Course> courses) {
        if (prerequisiteRepository.count() > 0) {
            return;
        }

        // Semester 2 prerequisites
        addPrereq(courses.get("INF108"), courses.get("CSS105"));
        addPrereq(courses.get("MAT138"), courses.get("MAT137"));
        addPrereq(courses.get("MAT151"), courses.get("MAT137"));
        addPrereq(courses.get("MDE170"), courses.get("MDE160"));
        addPrereq(courses.get("MDE191"), courses.get("MDE190"));
        addPrereq(courses.get("MDE292"), courses.get("MDE291"));

        // Semester 3 prerequisites
        addPrereq(courses.get("MAT251"), courses.get("MAT138"));
        addPrereq(courses.get("CSS215"), courses.get("CSS105"));
        addPrereq(courses.get("CSS215"), courses.get("INF108"));
        addPrereq(courses.get("CSS331"), courses.get("INF106"));
        addPrereq(courses.get("INF313"), courses.get("INF106"));
        addPrereq(courses.get("CSS118"), courses.get("CSS105"));
        addPrereq(courses.get("MDE293"), courses.get("MDE292"));

        // Semester 4 prerequisites
        addPrereq(courses.get("CSS217"), courses.get("CSS215"));
        addPrereq(courses.get("INF202"), courses.get("CSS105"));
        addPrereq(courses.get("INF995"), courses.get("INF108"));
        addPrereq(courses.get("INF203"), courses.get("INF313"));
        addPrereq(courses.get("INF211"), courses.get("INF108"));
        addPrereq(courses.get("MDE112"), courses.get("MDE111"));
        addPrereq(courses.get("MDE294"), courses.get("MDE293"));

        // Semester 5 prerequisites
        addPrereq(courses.get("INF381"), courses.get("INF114"));
        addPrereq(courses.get("CSS280"), courses.get("INF211"));
        addPrereq(courses.get("INF317"), courses.get("MAT251"));
        addPrereq(courses.get("INF317"), courses.get("INF202"));
        addPrereq(courses.get("INF777"), courses.get("INF114"));
        addPrereq(courses.get("INF330"), courses.get("CSS331"));
        addPrereq(courses.get("INF345"), courses.get("CSS331"));
        addPrereq(courses.get("INF345"), courses.get("INF313"));
        addPrereq(courses.get("INF318"), courses.get("INF108"));
        addPrereq(courses.get("INF321"), courses.get("MAT251"));
        addPrereq(courses.get("INF321"), courses.get("CSS118"));
        addPrereq(courses.get("INF429"), courses.get("INF381"));
        addPrereq(courses.get("INF429"), courses.get("INF114"));
        addPrereq(courses.get("INF316"), courses.get("INF995"));
        addPrereq(courses.get("CSS216"), courses.get("INF108"));
        addPrereq(courses.get("INF314"), courses.get("INF313"));
        addPrereq(courses.get("INF888"), courses.get("INF202"));
        addPrereq(courses.get("INF357"), courses.get("INF108"));
        addPrereq(courses.get("INF319"), courses.get("CSS105"));
        addPrereq(courses.get("INF319"), courses.get("INF108"));
        addPrereq(courses.get("INF319"), courses.get("CSS215"));
        addPrereq(courses.get("INF347"), courses.get("INF203"));
        addPrereq(courses.get("INF999"), courses.get("MAT138"));

        // Semester 6 prerequisites
        addPrereq(courses.get("CSS410"), courses.get("INF114"));
        addPrereq(courses.get("INF395"), courses.get("CSS217"));
        addPrereq(courses.get("INF395"), courses.get("INF202"));
        addPrereq(courses.get("INF324"), courses.get("CSS118"));
        addPrereq(courses.get("INF305"), courses.get("INF202"));
        addPrereq(courses.get("INF373"), courses.get("INF995"));
        addPrereq(courses.get("INF389"), courses.get("INF114"));
        addPrereq(courses.get("INF234"), courses.get("CSS216"));
        addPrereq(courses.get("INF390"), courses.get("INF345"));
        addPrereq(courses.get("INF337"), courses.get("INF321"));
        addPrereq(courses.get("INF374"), courses.get("INF114"));
        addPrereq(courses.get("INF392"), courses.get("INF203"));
        addPrereq(courses.get("INF368"), courses.get("INF108"));
        addPrereq(courses.get("INF998"), courses.get("INF321"));
        addPrereq(courses.get("INF332"), courses.get("INF114"));
        addPrereq(courses.get("INF376"), courses.get("CSS118"));
        addPrereq(courses.get("INF300"), courses.get("INF114"));
        addPrereq(courses.get("INF378"), courses.get("CSS331"));
        addPrereq(courses.get("INF378"), courses.get("INF313"));
        addPrereq(courses.get("INF356"), courses.get("INF357"));
        addPrereq(courses.get("INF997"), courses.get("INF202"));
        addPrereq(courses.get("INF377"), courses.get("INF318"));

        // Semester 7 prerequisites
        addPrereq(courses.get("CSS483"), courses.get("CSS280"));
        addPrereq(courses.get("INF387"), courses.get("INF381"));
        addPrereq(courses.get("INF406"), courses.get("INF202"));
        addPrereq(courses.get("INF407"), courses.get("INF114"));
        addPrereq(courses.get("INF415"), courses.get("INF381"));
        addPrereq(courses.get("CSS314"), courses.get("CSS331"));
        addPrereq(courses.get("INF412"), courses.get("INF381"));
        addPrereq(courses.get("INF412"), courses.get("INF114"));
        addPrereq(courses.get("INF414"), courses.get("INF381"));
        addPrereq(courses.get("INF414"), courses.get("INF114"));
        addPrereq(courses.get("INF996"), courses.get("INF202"));

        // Semester 8 prerequisites
        addPrereq(courses.get("INF420"), courses.get("INF395"));
        addPrereq(courses.get("INF420"), courses.get("CSS410"));
        addPrereq(courses.get("INF459"), courses.get("INF381"));
        addPrereq(courses.get("CSS486"), courses.get("CSS483"));
        addPrereq(courses.get("CSS495"), courses.get("CSS410"));
        addPrereq(courses.get("CSS479"), courses.get("INF420"));
    }

    private void addPrereq(Course target, Course required) {
        if (target != null && required != null) {
            Prerequisite prereq = new Prerequisite();
            prereq.setCourse(target);
            prereq.setPrerequisiteCourse(required);
            prerequisiteRepository.save(prereq);
        }
    }

    private void seedCurriculum(Major major, int catalogYear, Map<String, Course> courses) {
        log.info("Seeding SDU Information Systems BSc. curriculum for major {} (catalog year {})...", major.getCode(), catalogYear);
        for (CourseSeed seed : seededCatalogList) {
            addCurriculum(major, courses.get(seed.code()), seed.semester(), catalogYear, seed.courseType());
        }
    }

    private void addCurriculum(Major major, Course course, int semester, int catalogYear, String type) {
        if (major != null && course != null
                && !curriculumCourseRepository.existsByMajor_IdAndCourse_IdAndCatalogYear(
                        major.getId(), course.getId(), catalogYear)) {
            curriculumCourseRepository.save(new CurriculumCourse(major, course, semester, catalogYear, type));
        }
    }

    private void seedDemoStudent(Major major, Map<String, Course> courses) {
        String studentId = "240103000";
        String email = "240103000@sdu.edu.kz";

        StudentAccount account = studentAccountRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            StudentAccount acc = new StudentAccount();
            acc.setEmail(email);
            acc.setStudentId(studentId);
            acc.setPasswordHash(passwordEncoder.encode("Student123!@#"));
            return studentAccountRepository.save(acc);
        });
        account.setMajor(major);
        if (account.getCatalogYear() == null) {
            account.setCatalogYear(2024);
        }
        studentAccountRepository.save(account);

        Optional<Student> existingStudent = studentRepository.findByStudentId(studentId);
        if (existingStudent.isPresent()) {
            Student student = existingStudent.get();
            student.setMajor(major);
            if (student.getAccount() == null) {
                student.setAccount(account);
            }
            if (student.getPasswordHash() == null) {
                student.setPasswordHash(account.getPasswordHash());
            }
            if (student.getCareerGoal() == null) {
                student.setCareerGoal("Backend & Cloud Engineering");
            }
            studentRepository.save(student);
        } else {
            log.info("Seeding demo student: {}", studentId);
            Student student = new Student();
            student.setStudentId(studentId);
            student.setName("John Doe");
            student.setEmail(email);
            student.setPasswordHash(account.getPasswordHash());
            student.setGpa(3.8);
            student.setMajor(major);
            student.setCatalogYear(2024);
            student.setCurrentSemester(3);
            student.setCareerGoal("Backend & Cloud Engineering");
            student.setAccount(account);

            // Completed courses: Semesters 1 and 2 (8 courses = 41 ECTS)
            student.setCompletedCourses(Set.of(
                    courses.get("INF106"),
                    courses.get("CSS105"),
                    courses.get("MAT137"),
                    courses.get("MAT156"),
                    courses.get("MDE171"),
                    courses.get("INF114"),
                    courses.get("INF108"),
                    courses.get("MAT138")
            ));

            // Currently enrolled courses: Semester 3 (4 courses = 20 ECTS)
            student.setEnrolledCourses(Set.of(
                    courses.get("MAT251"),
                    courses.get("CSS215"),
                    courses.get("CSS331"),
                    courses.get("INF313")
            ));

            studentRepository.save(student);
            account.setStudent(student);
            log.info("Demo student seeded with major IS (SITAM), 41 completed ECTS, 20 enrolled ECTS");
        }
    }

    private record CourseSeed(
            String code,
            String name,
            int credits,
            int difficulty,
            int semester,
            String courseType,
            String description
    ) {}
}
