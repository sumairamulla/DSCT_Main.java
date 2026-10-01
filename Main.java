import java.io.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

enum ActivityType {
    ACADEMIC("Academic"),
    NON_ACADEMIC("Non-Academic");

    private final String label;

    ActivityType(String label) { this.label = label; }

    public String getLabel() { return label; }
}

enum Program {
    MCA("Master of Computer Applications", 2),
    MSC_AI("M.Sc. Artificial Intelligence", 2),
    MSC_DS("M.Sc. Data Science", 2),
    MA_ECONOMICS("M.A. Economics", 2),
    MCOM("M.Com", 2),
    MBA("Master of Business Administration", 2),
    INT_DATA_SCIENCE("Integrated Data Science", 5),
    INT_MBA("Integrated MBA", 5);

    private final String title;
    private final int durationYears;

    Program(String title, int durationYears) {
        this.title = title;
        this.durationYears = durationYears;
    }

    @Override
    public String toString() { return title + " (" + durationYears + " yrs)"; }
}

class InvalidDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidDataException(String message) {
        super(message);
    }
}

// Common interface used for NIRF-style reporting.
interface Reportable {

    String report();

    default String header() {
        return "[NIRF-REPORT]";
    }
}

// Address is Cloneable and Serializable for cloning and persistence.
class Address implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private String city;
    private String state;

    public Address(String city, String state) {
        this.city = city;
        this.state = state;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    @Override
    public Address clone() {
        try {
            return (Address) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String toString() { return city + ", " + state; }
}

// Base class for students and staff; also demonstrates static counting and inheritance.
abstract class Member implements Serializable, Reportable, Cloneable, Comparable<Member> {
    private static final long serialVersionUID = 1L;

    private static int count = 0;

    private final int id;
    private String name;
    protected String email;
    public String department = "CST, DCST, Goa University";

    protected Member(String name, String email) throws InvalidDataException {
        if (name == null || name.isBlank()) {
            throw new InvalidDataException("Name cannot be empty");
        }
        if (email == null || !email.contains("@")) {
            throw new InvalidDataException("Invalid e-mail for " + name);
        }
        this.name = name;
        this.email = email;
        this.id = ++count;
    }

    public static void displayCount() {
        System.out.println("Total members created so far: " + count);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public abstract String role();

    @Override
    public String report() {
        return header() + " #" + id + " " + role() + ": " + name;
    }

    @Override
    public int compareTo(Member other) {
        return this.name.compareToIgnoreCase(other.name);
    }

    @Override
    public String toString() { return report(); }
}

// Student demonstrates inheritance, constructor use, overloading and cloning.
class Student extends Member {
    private static final long serialVersionUID = 1L;

    private final String rollNo;
    private Program program;
    private int batchYear;
    private Address address;
    private ArrayList<String> subjects = new ArrayList<>();
    private transient String sessionNote = "temp";

    public Student(String name, String email, String rollNo, Program program,
                   int batchYear, Address address) throws InvalidDataException {
        super(name, email);
        if (rollNo == null || rollNo.isBlank()) {
            throw new InvalidDataException("Roll number required for " + name);
        }
        this.rollNo = rollNo;
        this.program = program;
        this.batchYear = batchYear;
        this.address = address;
    }

    public void enroll(String subject) { subjects.add(subject); }
    public void enroll(String subject, String semester) { subjects.add(subject + " (" + semester + ")"); }
    public void enroll(List<String> many) { subjects.addAll(many); }

    public Address getAddress() { return address; }
    public List<String> getSubjects() { return subjects; }
    public Program getProgram() { return program; }
    public int getBatchYear() { return batchYear; }
    public String getSessionNote() { return sessionNote; }

    @Override public String role() { return "Student"; }

    @Override
    public String report() {
        return super.report() + " | Roll " + rollNo + " | " + program.name() + " | Batch " + batchYear;
    }

    @Override
    public Student clone() {
        try {
            return (Student) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public Student deepClone() {
        Student copy = this.clone();
        copy.address = this.address.clone();
        copy.subjects = new ArrayList<>(this.subjects);
        return copy;
    }
}

// Teaching staff is another derived class for runtime polymorphism.
class TeachingStaff extends Member {
    private static final long serialVersionUID = 1L;
    private String designation;
    private String specialization;

    public TeachingStaff(String name, String email, String designation, String specialization)
            throws InvalidDataException {
        super(name, email);
        this.designation = designation;
        this.specialization = specialization;
    }

    @Override public String role() { return "Teaching Staff"; }

    @Override
    public String report() {
        return super.report() + " | " + designation + " | " + specialization;
    }
}

// Non-teaching staff completes the member hierarchy.
class NonTeachingStaff extends Member {
    private static final long serialVersionUID = 1L;
    private String post;

    public NonTeachingStaff(String name, String email, String post) throws InvalidDataException {
        super(name, email);
        this.post = post;
    }

    @Override public String role() { return "Non-Teaching Staff"; }

    @Override
    public String report() { return super.report() + " | " + post; }
}

// Course stores programme and assessment information used in the NIRF model.
class Course implements Reportable {

    public static final int MARKS_PER_CREDIT = 25;

    private final String code;
    private final String title;
    private final int credits;
    private final Program program;
    private final int semester;

    public Course(String code, String title, int credits, Program program, int semester) {
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.program = program;
        this.semester = semester;
    }

    public int totalMarks() { return credits * MARKS_PER_CREDIT; }

    public static int bestTwoOfThree(int isa1, int isa2, int isa3) {
        return isa1 + isa2 + isa3 - Math.min(isa1, Math.min(isa2, isa3));
    }

    @Override
    public String report() {
        return header() + " Course " + code + " - " + title + " | " + credits + " cr | "
                + totalMarks() + " marks | " + program.name() + " Sem " + semester;
    }
}

// Base activity class for academic and non-academic activities.
abstract class Activity implements Reportable, Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final int year;
    private final ActivityType type;

    protected Activity(String name, int year, ActivityType type) {
        this.name = name;
        this.year = year;
        this.type = type;
    }

    public String getName() { return name; }
    public int getYear() { return year; }
    public ActivityType getType() { return type; }

    public abstract String details();

    @Override
    public String report() {
        return header() + " [" + type.getLabel() + "] " + year + " - " + name + " : " + details();
    }
}

class AcademicActivity extends Activity {
    private static final long serialVersionUID = 1L;
    private final String outcome;

    public AcademicActivity(String name, int year, String outcome) {
        super(name, year, ActivityType.ACADEMIC);
        this.outcome = outcome;
    }

    @Override
    public String details() {
        return outcome;
    }
}

class NonAcademicActivity extends Activity {
    private static final long serialVersionUID = 1L;
    private final String organisedBy;

    public NonAcademicActivity(String name, int year, String organisedBy) {
        super(name, year, ActivityType.NON_ACADEMIC);
        this.organisedBy = organisedBy;
    }

    @Override
    public String details() {
        return "Organised by " + organisedBy;
    }
}

// Generic repository demonstrates bounded generics and collection processing.
class Repository<T extends Member> {
    private final List<T> items = new ArrayList<>();

    public void add(T item) { items.add(item); }

    public int size() { return items.size(); }

    public List<T> filter(Predicate<T> condition) {
        List<T> result = new ArrayList<>();
        for (T t : items) {
            if (condition.test(t)) result.add(t);
        }
        return result;
    }

    public static <E extends Comparable<E>> E maxOf(List<E> list) {
        E best = list.get(0);
        for (E e : list) {
            if (e.compareTo(best) > 0) best = e;
        }
        return best;
    }

    public static void printAll(String title, Collection<?> c) {
        System.out.println(title + " (" + c.size() + "):");
        for (Object o : c) System.out.println("   - " + o);
    }
}

// Java supports multiple interface inheritance, demonstrated below.
class MultipleInheritanceDemo {

    interface Teacher {
        default String title() { return "Teacher"; }
        default String teach() { return "Teaching MCA/MSc courses"; }
        String subject();
    }

    interface Researcher {
        default String title() { return "Researcher"; }
        default String research() { return "Guiding Ph.D. scholars"; }
    }

    static class ProfessorProfile implements Teacher, Researcher {
        @Override public String subject() { return "Machine Learning"; }

        @Override
        public String title() {
            return Teacher.super.title() + " & " + Researcher.super.title();
        }
    }

    public static void run() {
        ProfessorProfile p = new ProfessorProfile();
        System.out.println("Title    : " + p.title());
        System.out.println("Teach    : " + p.teach());
        System.out.println("Research : " + p.research());
        System.out.println("Subject  : " + p.subject());
        System.out.println("""
                Difference:
                 * Implementation inheritance (extends)  : one parent class only; inherits fields + methods.
                 * Interface inheritance (implements)    : many parents; inherits a contract (and
                   optional default methods) but NO instance state.""");
    }
}

// Demonstrates public, protected and private access.
class AccessDemo {
    public String publicField = "public: visible everywhere";
    protected String protectedField = "protected: this class, subclasses, same package";
    private String privateField = "private: only inside AccessDemo";

    public static void insideDemo() {
        AccessDemo obj = new AccessDemo();
        System.out.println("INSIDE class  -> " + obj.publicField);
        System.out.println("INSIDE class  -> " + obj.protectedField);
        System.out.println("INSIDE class  -> " + obj.privateField);
    }

    public String getPrivateField() { return privateField; }
}

class AccessChild extends AccessDemo {
    public void show() {
        System.out.println("SUBCLASS      -> " + publicField);
        System.out.println("SUBCLASS      -> " + protectedField);
        System.out.println("SUBCLASS (via getter) -> " + getPrivateField());
    }
}

public class Main {

    public static void main(String[] args) {
        // Create sample members and data for the complete demonstration.
        try {
            Address panaji = new Address("Panaji", "Goa");
            Address margao = new Address("Margao", "Goa");

            Student s1 = new Student("Riya Naik", "riya@example.com", "2601", Program.MCA, 2026, panaji);
            Student s2 = new Student("Aarav Gaonkar", "aarav@example.com", "2602", Program.MSC_AI, 2026, margao);
            Student s3 = new Student("Sneha Kamat", "sneha@example.com", "2603", Program.INT_DATA_SCIENCE, 2026, panaji);

            TeachingStaff t1 = new TeachingStaff("Faculty Member 1", "hr@example.com",
                    "Assistant Professor", "NLP / Machine Learning");
            TeachingStaff t2 = new TeachingStaff("Faculty Member 2", "faculty@example.com",
                    "Associate Professor", "Databases");
            NonTeachingStaff n1 = new NonTeachingStaff("Lab Assistant 1", "lab@example.com", "Lab Assistant");

            s1.enroll("Java Programming");
            s1.enroll("Data Structures", "Sem 1");
            s2.enroll(Arrays.asList("Operating Systems", "DBMS"));

            // The following sections demonstrate all ten required OOP features.

// ======================= 1. OBJECT REFERENCING & CONSTRUCTORS =======================
            System.out.println("\n==================== 1. OBJECT REFERENCING & CONSTRUCTORS ====================");
            // Demonstrates shared references, constructors and reference side effects.
            feature1(s1, panaji, s3);

// ======================= 2. STATIC ATTRIBUTES, METHODS & ENUMS =======================
            System.out.println("\n==================== 2. STATIC ATTRIBUTES, METHODS & ENUMS ====================");
            // Demonstrates a static member counter and enum values.
            Member.displayCount();
            System.out.println("Enum values available: " + Program.values().length);

// ======================= 3. MULTIPLE INHERITANCE =======================
            System.out.println("\n==================== 3. MULTIPLE INHERITANCE ====================");
            // Demonstrates multiple interface inheritance and default methods.
            MultipleInheritanceDemo.run();

// ======================= 4. ACCESS MODIFIERS =======================
            System.out.println("\n==================== 4. ACCESS MODIFIERS ====================");
            // Demonstrates public, protected and private members.
            feature4();

// ======================= 5. ABSTRACT CLASSES & INTERFACES =======================
            System.out.println("\n==================== 5. ABSTRACT CLASSES & INTERFACES ====================");
            // Demonstrates abstract activity classes and the Reportable interface.
            List<Activity> activities = buildActivities();

            Activity academic = new AcademicActivity(
                    "ISA", 2026, "Academic assessment activity");
            Activity nonAcademic = new NonAcademicActivity(
                    "InfoFest", 2026, "Student activity");

            System.out.println("Abstract class -> AcademicActivity: "
                    + academic.details());
            System.out.println("Abstract class -> NonAcademicActivity: "
                    + nonAcademic.details());
            System.out.println("Interface -> Both activities implement Reportable.");
            System.out.println("NIRF activities stored: "
                    + activities.size() + " records (2024-2026).");

            assessmentDemo();

// ======================= 6. CLASS AS A TYPE & MESSAGE PASSING =======================
            System.out.println("\n==================== 6. CLASS AS A TYPE & MESSAGE PASSING ====================");
            // Demonstrates passing objects through the Reportable interface type.
            submitToNirf(s1);
            submitToNirf(t1);
            submitToNirf(activities.get(0));
            submitToNirf(new Course("CSC-501", "Advanced Algorithms", 4, Program.MCA, 3));

// ======================= 7. POLYMORPHISM =======================
            System.out.println("\n==================== 7. POLYMORPHISM ====================");
            // Demonstrates method overriding and method overloading.
            Member[] people = {s1, t1, n1};
            for (Member m : people) System.out.println("   Overriding: " + m.role() + " -> " + m.report());
            System.out.println("   Overloading: enroll(String), enroll(String,String), enroll(List) -> "
                    + s1.getSubjects() + " / " + s2.getSubjects());

// ======================= 8. GENERICS AND COLLECTION FRAMEWORKS =======================
            System.out.println("\n==================== 8. GENERICS AND COLLECTION FRAMEWORKS ====================");
            // Demonstrates generics with List, Queue, Set and Map.
            feature8(s1, s2, s3, t1, t2, activities);

// ======================= 9. EXCEPTION HANDLING =======================
            System.out.println("\n==================== 9. EXCEPTION HANDLING ====================");
            // Demonstrates custom and built-in exception handling.
            feature9();

// ======================= 10. REFLECTION, PERSISTENCE, I/O, CLONING =======================
            System.out.println("\n==================== 10. REFLECTION, PERSISTENCE, I/O, CLONING ====================");
            // Demonstrates RTTI, reflection, serialization, file/console I/O and cloning.
            feature10(s1, s2, s3, t1, n1, activities);

        } catch (InvalidDataException e) {
            System.out.println("Setup failed: " + e.getMessage());
        }
    }

    static void feature1(Student s1, Address panaji, Student s3) {
        Student alias = s1;
        alias.setName("Riya Naik (edited via alias)");
        System.out.println("s1.getName()  = " + s1.getName());
        System.out.println("s1 == alias ? " + (s1 == alias) + "   <- side effect: change via one reference is seen by the other");

        System.out.println("Before: s1 address = " + s1.getAddress() + ", s3 address = " + s3.getAddress());
        panaji.setCity("Porvorim");
        System.out.println("After : s1 address = " + s1.getAddress() + ", s3 address = " + s3.getAddress()
                + "   <- ... visible through both students");
        panaji.setCity("Panaji");
        s1.setName("Riya Naik");
    }

    static void feature4() {
        AccessDemo.insideDemo();

        AccessDemo outside = new AccessDemo();
        System.out.println("Outside class -> public    : accessible");
        System.out.println("Outside class -> protected : accessible (same package)");
        System.out.println("Outside class -> private   : accessed through getter");

        new AccessChild().show();
    }

    static List<Activity> buildActivities() {
        List<Activity> list = new ArrayList<>();
        for (int y = 2024; y <= 2026; y++) {
            list.add(new AcademicActivity("Orientation / Induction Programme", y, "Welcome and curriculum briefing for new batch"));
            list.add(new AcademicActivity("ISA (Internal Semester Assessment)", y, "3 ISAs per subject each semester; best 2 are counted"));
            list.add(new AcademicActivity("Midpoint Exam", y, "Conducted before the SEA in every semester"));
            list.add(new AcademicActivity("SEA (Semester End Assessment)", y, "Held at the end of every semester"));
            list.add(new AcademicActivity("Mini Project / Project Viva", y, "Project demo and viva-voce"));
            list.add(new AcademicActivity("Study Tour", y, "Industry visit with faculty"));
            list.add(new AcademicActivity("Campus Placement Drive", y, "Companies visited DCST for hiring"));
            list.add(new AcademicActivity("Internship", y, "Summer / final-semester industry internship"));
            list.add(new AcademicActivity("Workshop / Guest Lecture", y, "Expert sessions from industry"));
            list.add(new AcademicActivity("Seminar & Paper Presentation", y, "Student and faculty research talks"));
            list.add(new AcademicActivity("Hackathon / Coding Contest", y, "Competitive programming and problem solving"));
            list.add(new NonAcademicActivity("InfoFest", y, "DCST students"));
            list.add(new NonAcademicActivity("Plateaunia", y, "Goa University"));
            list.add(new NonAcademicActivity("Play-on", y, "DCST sports committee"));
            list.add(new NonAcademicActivity("Freshers' Welcome", y, "DCST students"));
            list.add(new NonAcademicActivity("Farewell", y, "DCST students"));
            list.add(new NonAcademicActivity("Sports Day", y, "Sports committee"));
            list.add(new NonAcademicActivity("Cultural Day / Annual Day", y, "Cultural committee"));
            list.add(new NonAcademicActivity("Blood Donation / NSS Camp", y, "NSS unit"));
            list.add(new NonAcademicActivity("Alumni Meet", y, "DCST alumni cell"));
        }
        return list;
    }

    static void assessmentDemo() {
        System.out.println("Assessment:");
        System.out.println("  2 credits = 50 marks | 3 credits = 75 marks | 4 credits = 100 marks");
        System.out.println("  ISA 14, 18, 16 -> Best two = " + Course.bestTwoOfThree(14, 18, 16));
        System.out.println("  Semester order: ISA -> Midpoint -> SEA");
    }

    static void submitToNirf(Reportable r) {
        System.out.println("   Submitting -> " + r.report());
    }

    static void feature8(Student s1, Student s2, Student s3, TeachingStaff t1, TeachingStaff t2,
                         List<Activity> activities) {

        Repository<Student> studentRepo = new Repository<>();
        studentRepo.add(s1);
        studentRepo.add(s2);
        studentRepo.add(s3);

        Repository<TeachingStaff> staffRepo = new Repository<>();
        staffRepo.add(t1);
        staffRepo.add(t2);

        System.out.println("Generic Repository -> Students: " + studentRepo.size()
                + ", Teaching Staff: " + staffRepo.size());

        List<Member> members = new ArrayList<>(List.of(s1, s2, s3, t1, t2));
        Collections.sort(members);
        System.out.println("List -> Members sorted by name: " + members.size());
        System.out.println("Generic method -> Last name alphabetically: "
                + Repository.maxOf(members).getName());

        Queue<Student> queue = new LinkedList<>(List.of(s1, s2, s3));
        List<String> queueNames = new ArrayList<>();
        for (Student s : queue) queueNames.add(s.getName());
        System.out.println("Queue -> Placement order: " + queueNames);

        Set<String> subjects = new TreeSet<>();
        subjects.addAll(s1.getSubjects());
        subjects.addAll(s2.getSubjects());
        System.out.println("Set -> Unique subjects: " + subjects);

        Map<Integer, Integer> activityCount = new TreeMap<>();
        for (Activity a : activities) {
            activityCount.merge(a.getYear(), 1, Integer::sum);
        }
        System.out.println("Map -> Activities by year: " + activityCount);
    }

    static void feature9() {
        try {
            new Student("", "bad@example.com", "9999", Program.MCA, 2026, null);
        } catch (InvalidDataException e) {
            System.out.println("Custom exception caught: " + e.getMessage());
        } finally {
            System.out.println("Finally block executed.");
        }

        try {
            int result = 10 / 0;
            System.out.println(result);
        } catch (ArithmeticException e) {
            System.out.println("ArithmeticException caught.");
        }

        try {
            int[] marks = new int[3];
            marks[5] = 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("ArrayIndexOutOfBoundsException caught.");
        }

        try {
            Integer.parseInt("abc");
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException caught.");
        }

        try {
            validateMarks(120);
        } catch (IllegalArgumentException e) {
            System.out.println("Thrown IllegalArgumentException caught.");
        }
    }

    static void validateMarks(int marks) {
        if (marks < 0 || marks > 100) throw new IllegalArgumentException("Marks must be 0-100, got " + marks);
    }

    // Reflection, persistence, file I/O and cloning are demonstrated together here.
    static void feature10(Student s1, Student s2, Student s3, TeachingStaff t1, NonTeachingStaff n1,
                          List<Activity> activities) {

        Member m = s1;
        System.out.println("RTTI -> Runtime class: " + m.getClass().getSimpleName());

        try {
            Class<?> c = Student.class;
            Field roll = c.getDeclaredField("rollNo");
            roll.setAccessible(true);

            Method role = c.getDeclaredMethod("role");
            role.setAccessible(true);

            System.out.println("Reflection -> Class: " + c.getSimpleName());
            System.out.println("Reflection -> Private field rollNo: " + roll.get(s1));
            System.out.println("Reflection -> role(): " + role.invoke(s1));
        } catch (ReflectiveOperationException e) {
            System.out.println("Reflection error: " + e.getMessage());
        }

        Student shallow = s1.clone();
        Student deep = s1.deepClone();

        s1.getAddress().setCity("CHANGED");
        s1.getSubjects().add("EXTRA-SUBJECT");

        System.out.println("Shallow clone -> shared address/subjects: "
                + shallow.getAddress() + " / " + shallow.getSubjects().size() + " subjects");
        System.out.println("Deep clone    -> independent address/subjects: "
                + deep.getAddress() + " / " + deep.getSubjects().size() + " subjects");

        s1.getAddress().setCity("Panaji");
        s1.getSubjects().remove("EXTRA-SUBJECT");

        List<Member> members = new ArrayList<>(List.of(s1, s2, s3, t1, n1));
        File dir = new File("data");
        dir.mkdirs();
        File store = new File(dir, "members.ser");

        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(store))) {
            out.writeObject(members);
            System.out.println("Persistence -> Saved " + members.size() + " members.");
        } catch (IOException e) {
            System.out.println("Persistence save error: " + e.getMessage());
        }

        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(store))) {
            @SuppressWarnings("unchecked")
            List<Member> loaded = (List<Member>) in.readObject();
            System.out.println("Persistence -> Loaded " + loaded.size() + " members.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Persistence load error: " + e.getMessage());
        }

        Path report = Paths.get("output", "NIRF_Report_DCST.txt");

        try {
            Files.createDirectories(report.getParent());

            try (PrintWriter pw =
                         new PrintWriter(new FileWriter(report.toFile()))) {

                pw.println("NIRF DATA - DCST, GBS, GOA UNIVERSITY (2024-2026)");
                pw.println("Programs: " + Program.values().length);
                pw.println("Academic activities per year: 11");
                pw.println("Non-academic activities per year: 9");
            }

            System.out.println("File I/O -> NIRF report created: "
                    + report);

        } catch (IOException e) {
            System.out.println("File I/O error: " + e.getMessage());
        }

        System.out.print("Console I/O -> Enter your name: ");
        Scanner sc = new Scanner(System.in);
        String user = sc.hasNextLine() ? sc.nextLine().trim() : "";

        if (user.isEmpty()) {
            user = "Student";
        }

        System.out.println("Hello, " + user + "!");

        System.out.println();
        System.out.println("--------------- NIRF DATA SUMMARY ---------------");

        System.out.println("Programs Offered:");
        for (Program p : Program.values()) {
            System.out.println("  - " + p);
        }

        System.out.println();
        System.out.println("Academic Activities (common list for 2024-2026):");
        for (Activity a : activities) {
            if (a.getYear() == 2026 && a.getType() == ActivityType.ACADEMIC) {
                System.out.println("  - " + a.getName());
            }
        }

        System.out.println();
        System.out.println("Non-Academic Activities (common list for 2024-2026):");
        for (Activity a : activities) {
            if (a.getYear() == 2026 && a.getType() == ActivityType.NON_ACADEMIC) {
                System.out.println("  - " + a.getName());
            }
        }

        System.out.println("-------------------------------------------------");
        Member.displayCount();
    }

    static List<Course> buildCourses() {
        return List.of(
            new Course("MCA-101", "Problem Solving and Programming", 4, Program.MCA, 1),
            new Course("MCA-102", "Linux", 3, Program.MCA, 1),
            new Course("MCA-103", "Operating Systems", 4, Program.MCA, 1),
            new Course("MCA-201", "Internet Technology", 4, Program.MCA, 2),
            new Course("MCA-202", "Object Oriented Technology", 4, Program.MCA, 2),
            new Course("MCA-203", "Web Development", 4, Program.MCA, 2),
            new Course("MCA-301", "Object Oriented Software Engineering", 4, Program.MCA, 3),
            new Course("MCA-302", "Cloud Computing", 4, Program.MCA, 3),
            new Course("AI-201", "Machine Learning", 4, Program.MSC_AI, 2),
            new Course("AI-202", "Deep Learning", 4, Program.MSC_AI, 2),
            new Course("DS-101", "Fundamentals of Data Science", 4, Program.MSC_DS, 1),
            new Course("DS-301", "Data Mining", 4, Program.MSC_DS, 3),
            new Course("ECO-101", "Microeconomic Theory", 4, Program.MA_ECONOMICS, 1),
            new Course("ECO-201", "Econometrics", 4, Program.MA_ECONOMICS, 2),
            new Course("COM-101", "Advanced Accounting", 4, Program.MCOM, 1),
            new Course("COM-201", "Corporate Finance", 4, Program.MCOM, 2),
            new Course("MBA-101", "Principles of Management", 4, Program.MBA, 1),
            new Course("MBA-201", "Financial Management", 4, Program.MBA, 2),
            new Course("IDS-101", "Mathematics", 4, Program.INT_DATA_SCIENCE, 1),
            new Course("IDS-202", "Fundamentals of Data Science", 4, Program.INT_DATA_SCIENCE, 2),
            new Course("IMBA-101", "Business Economics", 4, Program.INT_MBA, 1),
            new Course("IMBA-202", "Business Communication", 3, Program.INT_MBA, 2));
    }
}
