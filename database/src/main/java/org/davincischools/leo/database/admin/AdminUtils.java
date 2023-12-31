package org.davincischools.leo.database.admin;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.davincischools.leo.database.daos.Assignment;
import org.davincischools.leo.database.daos.ClassX;
import org.davincischools.leo.database.daos.District;
import org.davincischools.leo.database.daos.KnowledgeAndSkill;
import org.davincischools.leo.database.daos.ProjectDefinition;
import org.davincischools.leo.database.daos.ProjectDefinitionCategory;
import org.davincischools.leo.database.daos.ProjectDefinitionCategoryType;
import org.davincischools.leo.database.daos.ProjectInputCategory;
import org.davincischools.leo.database.daos.School;
import org.davincischools.leo.database.daos.TeacherSchool;
import org.davincischools.leo.database.daos.UserX;
import org.davincischools.leo.database.test.TestData;
import org.davincischools.leo.database.utils.Database;
import org.davincischools.leo.database.utils.UserUtils;
import org.davincischools.leo.database.utils.repos.KnowledgeAndSkillRepository.Type;
import org.davincischools.leo.database.utils.repos.ProjectDefinitionCategoryTypeRepository.ValueType;
import org.davincischools.leo.database.utils.repos.UserXRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(scanBasePackageClasses = {Database.class})
public class AdminUtils {
  public enum DaVinciSchoolsByNickname {
    DVC("Da Vinci Communications High School", "201 N. Douglas St., El Segundo, CA 90245"),
    DVConnect("Da Vinci Connect High School", "550 Continental Blvd., El Segundo, CA 90245"),
    DVD("Da Vinci Design High School", "201 N. Douglas St., El Segundo, CA 90245"),
    DVFlex("Da Vinci Flex High School", "Address TBD"),
    DVRISE("Da Vinci Rise High School-Richstone", "13634 Cordary Avenue, Hawthorne, CA 90250"),
    DVS("Da Vinci Science High School", "201 N. Douglas St., El Segundo, CA 90245");

    private final String name;
    private final String address;

    DaVinciSchoolsByNickname(String name, String address) {
      this.name = name;
      this.address = address;
    }

    public String getName() {
      return name;
    }

    public String getAddress() {
      return address;
    }

    public School createSchool(Database db, District district) {
      return db.getSchoolRepository()
          .upsert(district, this.name(), name, s -> s.setAddress(address));
    }
  }

  public enum XqCategoriesByNickname {
    ID(
        "Interdisciplinary",
        "Communication",
        "Create arguments, support claims, and engage in critical dialogue"),
    HUM(
        "Humanities",
        "Governments",
        "Cultivate, refine, and leverage perspectives on how governments work and how they could be"
            + " improved"),
    STEAM(
        "Science, Technology, Engineering, Arts, and Math (STEM)",
        "Scientific Thinking",
        "Work with quantitative data to understand, represent, and predict relationships"),
    SEL(
        "Social, Emotional Learning (SS)",
        "Relationship Building",
        "Build and maintain healthy relationships");

    private final String name;
    private final String exampleName;
    private final String exampleDescription;

    XqCategoriesByNickname(String name, String exampleName, String exampleDescription) {
      this.name = name;
      this.exampleName = exampleName;
      this.exampleDescription = exampleDescription;
    }

    public String getName() {
      return name;
    }

    public String getExampleName() {
      return exampleName;
    }

    public String getExampleDescription() {
      return exampleDescription;
    }
  }

  public static class ProjectDefinitionCategoryTypeBuilder {

    private static final AtomicInteger position = new AtomicInteger();

    private final Database db;
    private final ProjectDefinitionCategoryType type;

    public ProjectDefinitionCategoryTypeBuilder(Database db, ProjectDefinitionCategoryType type) {
      this.db = db;
      this.type = type;
    }

    public void addBothCategories(ProjectDefinition projectDefinition) {
      addProjectDefinitionCategory(projectDefinition, entry -> {});
      addProjectInputCategory(projectDefinition, entry -> {});
    }

    public ProjectDefinitionCategory addProjectDefinitionCategory(
        ProjectDefinition projectDefinition, Consumer<ProjectDefinitionCategory> modifier) {
      ProjectDefinitionCategory pdc =
          db.getProjectDefinitionCategoryRepository()
              .upsert(
                  projectDefinition,
                  type,
                  pdc2 -> pdc2.setPosition((float) position.getAndIncrement()).setMaxNumValues(4));
      modifier.accept(pdc);
      db.getProjectDefinitionCategoryRepository().saveAndFlush(pdc);
      return pdc;
    }

    @Deprecated
    public ProjectInputCategory addProjectInputCategory(
        ProjectDefinition projectDefinition, Consumer<ProjectInputCategory> modifier) {
      ProjectInputCategory pic =
          new ProjectInputCategory()
              .setCreationTime(Instant.now())
              .setName(type.getName())
              .setProjectDefinition(projectDefinition)
              .setPosition((float) position.getAndIncrement())
              .setShortDescr(type.getShortDescr())
              .setHint(type.getHint())
              .setInputDescr(type.getInputDescr())
              .setInputPlaceholder(type.getInputPlaceholder())
              .setQueryPrefix(type.getQueryPrefix())
              .setValueType(type.getValueType())
              .setMaxNumValues(4);
      modifier.accept(pic);
      db.getProjectInputCategoryRepository().saveAndFlush(pic);
      return pic;
    }
  }

  public static class ProjectDefinitionCategoryTypes {
    private final Database db;

    private final ProjectDefinitionCategoryTypeBuilder careerInterestType;
    private final ProjectDefinitionCategoryTypeBuilder motivationType;
    private final ProjectDefinitionCategoryTypeBuilder eksType;
    private final ProjectDefinitionCategoryTypeBuilder xqType;
    private final ProjectDefinitionCategoryTypeBuilder studentInterestsType;

    public ProjectDefinitionCategoryTypes(Database db) {
      this.db = db;

      careerInterestType =
          new ProjectDefinitionCategoryTypeBuilder(
              db,
              db.getProjectDefinitionCategoryTypeRepository()
                  .upsert(
                      "Career Interests",
                      type ->
                          type.setShortDescr("Career interests free text.")
                              .setHint("Click to add careers.")
                              .setInputDescr("Enter career interests:")
                              .setInputPlaceholder("Career Interest")
                              .setQueryPrefix("You are passionate about a career in")
                              .setValueType(ValueType.FREE_TEXT.name())));

      motivationType =
          new ProjectDefinitionCategoryTypeBuilder(
              db,
              db.getProjectDefinitionCategoryTypeRepository()
                  .upsert(
                      "Motivations",
                      type ->
                          type.setShortDescr("Motivation selections.")
                              .setHint("Click to add motivations.")
                              .setInputDescr("Select motivations:")
                              .setInputPlaceholder("Select a Motivation")
                              .setQueryPrefix("You are motivated by")
                              .setValueType(ValueType.MOTIVATION.name())));

      eksType =
          new ProjectDefinitionCategoryTypeBuilder(
              db,
              db.getProjectDefinitionCategoryTypeRepository()
                  .upsert(
                      "Knowledge and Skills",
                      type ->
                          type.setShortDescr("Knowledge and skill selections.")
                              .setHint("Click to add desired knowledge and skills.")
                              .setInputDescr("Select knowledge and skills:")
                              .setInputPlaceholder("Select a Knowledge and Skill")
                              .setQueryPrefix("You want to improve your ability to")
                              .setValueType(ValueType.EKS.name())));

      xqType =
          new ProjectDefinitionCategoryTypeBuilder(
              db,
              db.getProjectDefinitionCategoryTypeRepository()
                  .upsert(
                      "XQ Competencies",
                      type ->
                          type.setShortDescr("XQ competency selections.")
                              .setHint("Click to add desired XQ competency.")
                              .setInputDescr("Select XQ Competency:")
                              .setInputPlaceholder("Select a XQ Competency")
                              .setQueryPrefix("You want to improve your ability to")
                              .setValueType(ValueType.XQ_COMPETENCY.name())));

      studentInterestsType =
          new ProjectDefinitionCategoryTypeBuilder(
              db,
              db.getProjectDefinitionCategoryTypeRepository()
                  .upsert(
                      "Student Interests",
                      type ->
                          type.setShortDescr("Student interest free text.")
                              .setHint("Click to add student interests.")
                              .setInputDescr("Enter student interests:")
                              .setInputPlaceholder("Student Interest")
                              .setQueryPrefix("You are passionate about")
                              .setValueType(ValueType.FREE_TEXT.name())));
    }

    public ProjectDefinitionCategoryTypeBuilder getCareerInterestType() {
      return careerInterestType;
    }

    public ProjectDefinitionCategoryTypeBuilder getMotivationType() {
      return motivationType;
    }

    public ProjectDefinitionCategoryTypeBuilder getEksType() {
      return eksType;
    }

    public ProjectDefinitionCategoryTypeBuilder getXqType() {
      return xqType;
    }

    public ProjectDefinitionCategoryTypeBuilder getStudentInterestsType() {
      return studentInterestsType;
    }
  }

  private static final Joiner ERROR_JOINER = Joiner.on("\n - ");

  private static final Logger log = LogManager.getLogger();

  private record Error(String value, Exception e) {}

  private static String createPassword() {
    return new RandomStringGenerator.Builder()
        .withinRange(new char[] {'a', 'z'}, new char[] {'0', '9'})
        .build()
        .generate(20);
  }

  @Autowired private Database db;

  @Value("${createDistrict:}")
  private String createDistrict;

  @Value("${createAdmin:}")
  private List<String> createAdmins;

  @Value("${importTeachers:}")
  private String importTeachers;

  @Value("${importStudents:}")
  private String importStudents;

  @Value("${importXqEks:}")
  private String importXqEks;

  @Value("${importMotivations:}")
  private String importMotivations;

  @Value("${delimiter:[\\t]}")
  private String delimiter;

  @Value("${resetPassword:}")
  private List<String> resetPasswords;

  @Value("${loadTestData:false}")
  private String loadTestData;

  private District createDistrict() {
    checkArgument(createDistrict != null, "--createDistrict required.");

    return db.getDistrictRepository().upsert(createDistrict);
  }

  private void createAdmins() {
    checkArgument(!createAdmins.isEmpty(), "--createAdmin required.");

    for (String createAdmin : createAdmins) {
      String password = createPassword();
      District district = createDistrict();
      AtomicBoolean passwordUpdated = new AtomicBoolean(false);
      UserX userX =
          db.getUserXRepository()
              .upsert(
                  district,
                  createAdmin,
                  userX2 -> {
                    db.getAdminXRepository().upsert(userX2);
                    if (userX2
                        .getEncodedPassword()
                        .equals(UserXRepository.INVALID_ENCODED_PASSWORD)) {
                      UserUtils.setPassword(userX2, password);
                      passwordUpdated.set(true);
                    }
                  });
      addAdminToDistrictSchoolsAndClasses(db, userX);

      if (passwordUpdated.get()) {
        log.atWarn()
            .log(
                "IMPORTANT! Log in and change the temporary password:"
                    + " login: \"{}\", temporary password: \"{}\"",
                createAdmin,
                password);
      }
    }
  }

  // TODO: For now admins are teachers and students of all schools and classes in their district.
  public static void addAdminToDistrictSchoolsAndClasses(Database db, UserX admin) {
    db.getTeacherRepository().upsert(admin);
    db.getStudentRepository().upsert(admin, entity -> {});
    Set<Integer> schoolIds = new HashSet<>();
    for (School school : db.getSchoolRepository().findAll()) {
      if (Objects.equals(school.getDistrict().getId(), admin.getDistrict().getId())) {
        schoolIds.add(school.getId());
        db.getTeacherSchoolRepository().upsert(admin.getTeacher(), school);
      }
    }
    for (ClassX classX : db.getClassXRepository().findAll()) {
      if (schoolIds.contains(classX.getSchool().getId())) {
        db.getTeacherClassXRepository().upsert(admin.getTeacher(), classX);
        db.getStudentClassXRepository().upsert(admin.getStudent(), classX);
      }
    }
    db.getUserXRepository().save(admin);
  }

  private void resetPasswords() {
    checkArgument(!resetPasswords.isEmpty(), "--resetPassword required.");
    resetPasswords.stream()
        .parallel()
        .forEach(
            resetPassword -> {
              String password = createPassword();
              db.getUserXRepository()
                  .save(
                      UserUtils.setPassword(
                          db.getUserXRepository().findByEmailAddress(resetPassword).orElseThrow(),
                          password));
              log.atWarn()
                  .log(
                      "IMPORTANT! Log in and change the temporary password:"
                          + " login: \"{}\", temporary password: \"{}\"",
                      resetPassword,
                      password);
            });
  }

  private void importTeachers() throws IOException {
    checkArgument(importTeachers != null, "--importTeachers required.");

    District district = createDistrict();

    List<Error> errors = Collections.synchronizedList(new ArrayList<>());

    List<UserX> userXs =
        Files.readLines(new File(importTeachers), StandardCharsets.UTF_8).stream()
            .map(
                line -> {
                  try {
                    String[] cells = line.split(delimiter);
                    if (cells.length < 4) {
                      throw new IllegalArgumentException(
                          "Unexpected number of columns: " + cells.length + ".");
                    }

                    String firstName = cells[0];
                    String lastName = cells[1];
                    String emailAddress = cells[2];
                    String schoolNickname = cells[3];

                    checkArgument(!firstName.isEmpty(), "First name required.");
                    checkArgument(!lastName.isEmpty(), "Last name required.");
                    checkArgument(!emailAddress.isEmpty(), "Email address required.");
                    checkArgument(!schoolNickname.isEmpty(), "School nickname required.");
                    checkArgument(emailAddress.contains("@"), "Invalid e-mail address.");

                    UserX teacher =
                        db.getUserXRepository()
                            .upsert(
                                district,
                                emailAddress,
                                userX ->
                                    db.getTeacherRepository()
                                        .upsert(userX)
                                        .setFirstName(firstName)
                                        .setLastName(lastName));
                    School school =
                        DaVinciSchoolsByNickname.valueOf(schoolNickname).createSchool(db, district);
                    db.getTeacherSchoolRepository().upsert(teacher.getTeacher(), school);

                    if (cells.length >= 5) {
                      String className = cells[4];

                      checkArgument(!className.isEmpty(), "Class name required.");

                      ClassX classX = db.getClassXRepository().upsert(school, className, c -> {});
                      db.getTeacherClassXRepository().upsert(teacher.getTeacher(), classX);

                      for (int eksIndex : new int[] {5, 7}) {
                        if (cells.length >= eksIndex + 2) {
                          String eksName = cells[eksIndex];
                          String eksDescr = cells[eksIndex + 1];

                          checkArgument(!eksName.isEmpty(), "EKS name required.");
                          checkArgument(!eksDescr.isEmpty(), "EKS description required.");

                          KnowledgeAndSkill eks =
                              db.getKnowledgeAndSkillRepository()
                                  .upsert(eksName, Type.EKS, ks -> ks.setShortDescr(eksDescr));
                          db.getClassXKnowledgeAndSkillRepository().upsert(classX, eks);
                        }
                      }
                    }

                    log.atTrace().log("Imported: {}", line);

                    return teacher;
                  } catch (Exception e) {
                    log.atError().withThrowable(e).log("Error: {}", line);
                    errors.add(new Error(line, e));
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .parallel()
            .peek(
                userX -> {
                  if (userX.getEncodedPassword().equals(UserXRepository.INVALID_ENCODED_PASSWORD)) {
                    UserUtils.setPassword(userX, userX.getEmailAddress());
                  }
                })
            .toList();

    db.getUserXRepository().saveAllAndFlush(userXs);

    if (!errors.isEmpty()) {
      log.atError()
          .log(
              "There were errors during the import of the following teachers:\n - {}\n",
              ERROR_JOINER.join(
                  Lists.transform(errors, e -> e.value + "  --  " + e.e.getMessage())));
    }

    log.atInfo().log("Done importing teachers.");
  }

  private void importStudents() throws IOException {
    checkArgument(importStudents != null, "--importStudents required.");

    District district = createDistrict();

    List<Error> errors = Collections.synchronizedList(new ArrayList<>());

    List<UserX> students =
        Files.readLines(new File(importStudents), StandardCharsets.UTF_8).stream()
            .map(
                line -> {
                  try {
                    String[] cells = line.split(delimiter);
                    if (cells.length < 6) {
                      throw new IllegalArgumentException(
                          "Unexpected number of columns: " + cells.length + ".");
                    }

                    int id = Integer.parseInt(cells[0]);
                    int grade = Integer.parseInt(cells[1]);
                    String lastName = cells[2];
                    String firstName = cells[3];
                    String emailAddress = cells[4];
                    String schoolNickname = cells[5];

                    checkArgument(!lastName.isEmpty(), "Last name required.");
                    checkArgument(!firstName.isEmpty(), "First name required.");
                    checkArgument(!emailAddress.isEmpty(), "Email address required.");
                    checkArgument(!schoolNickname.isEmpty(), "School nickname required.");

                    UserX student =
                        db.getUserXRepository()
                            .upsert(
                                district,
                                emailAddress,
                                userX ->
                                    db.getStudentRepository()
                                        .upsert(
                                            userX, u -> u.setDistrictStudentId(id).setGrade(grade))
                                        .setFirstName(firstName)
                                        .setLastName(lastName));

                    School school =
                        DaVinciSchoolsByNickname.valueOf(schoolNickname).createSchool(db, district);
                    db.getStudentSchoolRepository().upsert(student.getStudent(), school);

                    db.getClassXRepository()
                        .findAllBySchool(school)
                        .forEach(
                            classX ->
                                db.getStudentClassXRepository()
                                    .upsert(student.getStudent(), classX));

                    log.atTrace().log("Imported: {}", line);

                    return student;
                  } catch (Exception e) {
                    log.atError().withThrowable(e).log("Error: {}", line);
                    errors.add(new Error(line, e));
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .parallel()
            .peek(
                userX -> {
                  if (userX.getEncodedPassword().equals(UserXRepository.INVALID_ENCODED_PASSWORD)) {
                    UserUtils.setPassword(
                        userX, userX.getLastName() + userX.getStudent().getDistrictStudentId());
                  }
                })
            .toList();

    db.getUserXRepository().saveAllAndFlush(students);

    if (!errors.isEmpty()) {
      log.atError()
          .log(
              "There were errors during the import of the following students:\n - {}\n",
              ERROR_JOINER.join(
                  Iterables.transform(errors, e -> e.value + "  --  " + e.e.getMessage())));
    }

    log.atInfo().log("Done importing students.");
  }

  private void importXqEks() throws IOException {
    checkArgument(importXqEks != null, "--importXqEks required.");

    District district = createDistrict();
    School school = DaVinciSchoolsByNickname.DVRISE.createSchool(db, district);

    List<Error> errors = Collections.synchronizedList(new ArrayList<>());
    Set<Integer> classXIds = new HashSet<>();

    for (String line : Files.readLines(new File(importXqEks), StandardCharsets.UTF_8)) {
      try {
        String[] cells = line.split(delimiter);
        if (cells.length != 4) {
          throw new IllegalArgumentException("Unexpected number of columns: " + cells.length + ".");
        }

        String xqName = cells[0];
        String xqDescr = cells[1];
        String outcome = cells[2];
        XqCategoriesByNickname xqCategory = XqCategoriesByNickname.valueOf(cells[3]);

        checkArgument(!xqName.isEmpty(), "Title required.");
        checkArgument(!xqDescr.isEmpty(), "Description required.");
        checkArgument(!outcome.isEmpty(), "Outcome required.");

        KnowledgeAndSkill knowledgeAndSkill =
            db.getKnowledgeAndSkillRepository()
                .upsert(
                    xqCategory.getName() + ": " + xqName,
                    Type.XQ_COMPETENCY,
                    ks -> ks.setShortDescr(xqDescr));

        ClassX classX =
            db.getClassXRepository().upsert(school, xqName, c -> c.setShortDescr(xqDescr));
        db.getClassXKnowledgeAndSkillRepository().upsert(classX, knowledgeAndSkill);

        Assignment assignment =
            db.getAssignmentRepository().upsert(classX, xqName, a -> a.setShortDescr(xqDescr));
        db.getAssignmentKnowledgeAndSkillRepository().upsert(assignment, knowledgeAndSkill);

        classXIds.add(classX.getId());

        log.atTrace().log("Imported: {}", line);
      } catch (Exception e) {
        log.atError().withThrowable(e).log("Error: {}", line);
        errors.add(new Error(line, e));
      }
    }
    try {
      for (TeacherSchool teacherSchool : db.getTeacherSchoolRepository().findAll()) {
        if (teacherSchool.getSchool().getId().equals(school.getId())) {
          for (int classXId : classXIds) {
            ClassX classX = new ClassX().setId(classXId);
            db.getTeacherClassXRepository().upsert(teacherSchool.getTeacher(), classX);
          }
        }
      }
    } catch (Exception e) {
      log.atError().withThrowable(e).log("Error: adding teachers to classes.");
      errors.add(new Error("N/A", e));
    }

    if (!errors.isEmpty()) {
      log.atError()
          .log(
              "There were errors during the import of the following XQ EKS':\n - {}\n",
              ERROR_JOINER.join(
                  Lists.transform(errors, e -> e.value + "  --  " + e.e.getMessage())));
    }

    log.atInfo().log("Done importing XQ EKS'.");
  }

  private void importMotivations() throws IOException {
    checkArgument(importMotivations != null, "--importMotivations required.");

    List<Error> errors = Collections.synchronizedList(new ArrayList<>());

    for (String line : Files.readLines(new File(importMotivations), StandardCharsets.UTF_8)) {
      try {
        String[] cells = line.split(delimiter);
        if (cells.length != 2) {
          throw new IllegalArgumentException("Unexpected number of columns: " + cells.length + ".");
        }

        String title = cells[0];
        String descr = cells[1];

        checkArgument(!title.isEmpty(), "Title required.");
        checkArgument(!descr.isEmpty(), "Description required.");

        checkArgument(!title.equals("Title"), "Header row.");

        db.getMotivationRepository().upsert(title, descr, m -> {});

        log.atTrace().log("Imported: {}", line);
      } catch (Exception e) {
        log.atError().withThrowable(e).log("Error: {}", line);
        errors.add(new Error(line, e));
      }
    }

    if (!errors.isEmpty()) {
      log.atError()
          .log(
              "There were errors during the import of the following motivations:\n - {}\n",
              ERROR_JOINER.join(
                  Lists.transform(errors, e -> e.value + "  --  " + e.e.getMessage())));
    }

    log.atInfo().log("Done importing motivations.");
  }

  public static void addIkigaiDiagramDescriptions(
      Database db, UserX creator, Iterable<Assignment> assignments) {
    log.atInfo().log("Creating Ikigai Diagram descriptions");

    ProjectDefinitionCategoryTypes types = new ProjectDefinitionCategoryTypes(db);

    // Create project definitions.
    ProjectDefinition ksDefinition =
        db.getProjectDefinitionRepository()
            .upsert(
                "Knowledge and Skills + Motivations",
                creator,
                def -> def.setCreationTime(Instant.now()));
    types.getCareerInterestType().addBothCategories(ksDefinition);
    types.getMotivationType().addBothCategories(ksDefinition);
    types.getEksType().addBothCategories(ksDefinition);
    types.getStudentInterestsType().addBothCategories(ksDefinition);

    ProjectDefinition xqDefinition =
        db.getProjectDefinitionRepository()
            .upsert("XQ Competencies", creator, def -> def.setCreationTime(Instant.now()));
    types.getCareerInterestType().addBothCategories(xqDefinition);
    types.getXqType().addBothCategories(xqDefinition);
    types.getStudentInterestsType().addBothCategories(xqDefinition);

    ProjectDefinition requirementDefinition =
        db.getProjectDefinitionRepository()
            .upsert("Requirements Template", creator, pd -> pd.setTemplate(true));
    types.getEksType().addBothCategories(requirementDefinition);
    types.getXqType().addBothCategories(requirementDefinition);
    types.getMotivationType().addBothCategories(requirementDefinition);
    types.getStudentInterestsType().addBothCategories(requirementDefinition);

    int assignmentCount = 0;
    for (Assignment assignment : assignments) {
      ++assignmentCount;
      int projectDefinitionCount = 0;
      for (ProjectDefinition projectDefinition :
          new ProjectDefinition[] {ksDefinition, xqDefinition, requirementDefinition}) {
        ++projectDefinitionCount;
        var apd =
            db.getAssignmentProjectDefinitionRepository()
                .upsert(assignment, projectDefinition, entity -> {});
        if ((assignmentCount % 3) == (projectDefinitionCount % 3)) {
          db.getAssignmentProjectDefinitionRepository()
              .saveAndFlush(apd.setSelected(Instant.now()));
        }
      }
    }

    log.atInfo().log("Done creating Ikigai Diagram descriptions");
  }

  private void processCommands() throws IOException {
    if (!createDistrict.isEmpty()) {
      log.atInfo().log("Creating district: {}", createDistrict);
      District district = createDistrict();

      // For now, just create one of each school.
      Arrays.stream(DaVinciSchoolsByNickname.values())
          .forEach(dvs -> dvs.createSchool(db, district));
    }

    if (!importTeachers.isEmpty()) {
      log.atInfo().log("Importing teachers: {}", importTeachers);
      importTeachers();
    }
    if (!importStudents.isEmpty()) {
      log.atInfo().log("Importing students: {}", importStudents);
      importStudents();
    }
    if (!importXqEks.isEmpty()) {
      log.atInfo().log("Importing XQ EKS: {}", importXqEks);
      importXqEks();
    }
    if (!importMotivations.isEmpty()) {
      log.atInfo().log("Importing Motivations: {}", importMotivations);
      importMotivations();
    }
    if (!createAdmins.isEmpty()) {
      log.atInfo().log("Creating admin: {}", createAdmins);
      createAdmins();
    }
    if (!Objects.equals(loadTestData, "false")) {
      log.atInfo().log("Loading test data");
      new TestData(db).addTestData();
    } else {
      checkArgument(!createAdmins.isEmpty(), "--createAdmin required.");
      UserX userX = db.getUserXRepository().findByEmailAddress(createAdmins.get(0)).orElseThrow();
      addIkigaiDiagramDescriptions(db, userX, db.getAssignmentRepository().findAll());
    }
    if (!resetPasswords.isEmpty()) {
      log.atInfo().log("Resetting passwords: {}", resetPasswords);
      resetPasswords();
    }
  }

  public static void main(String[] argsArray) throws IOException {
    try {
      ApplicationContext context = SpringApplication.run(AdminUtils.class, argsArray);
      context.getBean(AdminUtils.class).processCommands();
      System.exit(0);
    } catch (Exception e) {
      log.atError().withThrowable(e).log("Failed to execute commands.");
    }
  }
}
