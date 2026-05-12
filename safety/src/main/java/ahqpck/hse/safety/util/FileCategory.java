package ahqpck.hse.safety.util;

public enum FileCategory {

    USER_PROFILE_IMAGE        ("user-profile/images",        FileType.IMAGE),
    INCIDENT_IMAGE            ("incident/images",            FileType.IMAGE),
    INCIDENT_DOCUMENT         ("incident/documents",         FileType.DOCUMENT),
    OBSERVATION_IMAGE         ("observation/images",         FileType.IMAGE),
    OBSERVATION_DOCUMENT      ("observation/documents",      FileType.DOCUMENT),
    TOOLBOX_MEETING_IMAGE     ("toolbox-meeting/images",     FileType.IMAGE),
    TOOLBOX_MEETING_DOCUMENT  ("toolbox-meeting/documents",  FileType.DOCUMENT),
    WORK_INSTRUCTION_DOCUMENT ("work-instruction/documents", FileType.DOCUMENT),
    RISK_ASSESSMENT_DOCUMENT  ("risk-assessment/documents",  FileType.DOCUMENT),
    WORK_PERMIT_DOCUMENT      ("work-permit/documents",      FileType.DOCUMENT);

    private final String path;
    private final FileType fileType;

    FileCategory(String path, FileType fileType) {
        this.path = path;
        this.fileType = fileType;
    }

    public String getPath() { return path; }
    public FileType getFileType() { return fileType; }

    public enum FileType { IMAGE, DOCUMENT }
}