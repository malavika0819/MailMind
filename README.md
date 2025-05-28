MailMinder is your personal assistant for managing important emails by scheduling remainders and setting priorites ,ensuring you never miss a critical deadlines
 It securely connects to your Gmail account, allows you to view your emails, and then use MailMinder's features to stay organized and on top of your communications.
  Features
 Secure Google OAuth 2.0 Login: Sign in and connect your Gmail account securely.
 Real-Time Email Fetching: Displays a feed of your recent/important emails directly from your connected Gmail account.# Dynamic Email Dashboard:** View key information (Date, Sender, Subject, Snippet) for your emails.
 Priority Setting: Mark important emails with high, medium, or low priority for better focus. This information is stored within MailMinder.
 Reminder Scheduling: Set specific dates, times, and custom notes for reminders on crucial emails. This information is stored within MailMinder.
 Centralized Reminder Management: (Planned/Partially Implemented) A dedicated view to see all your upcoming reminders.
 Email Notifications for Reminders:** (Planned) Receive email notifications from MailMinder when your scheduled reminders are due.
 Modern & Responsive UI:** A clean user interface for managing your emails and reminders.

# Getting Started

 These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

# Prerequisites
 Java Development Kit (JDK): Version 17 or higher.# Apache Maven: Version 3.6.x or higher (or use the included Maven Wrapper `mvnw`).
MySQL Server: Version 8.0.x or higher# Google Cloud Platform (GCP) Account: To set up OAuth 2.0 credentials and enable the Gmail API.
 An IDE: IntelliJ IDEA, Eclipse, or VS Code with Java and Spring Boot extensions.

# Installation & Setup
# 1.Clone the repository:
    ```bash
    git clone https://github.com/YOUR_USERNAME/minder.git 
    cd minder
    ```
    *(Replace `YOUR_USERNAME/minder.git` with your actual repository URL)*

 # 2. Create MySQL Database:
     Connect to your local MySQL server (e.g., using MySQL Workbench).
       Create a database named `mailmind_db`:
        ```sql
        CREATE DATABASE mailmind_db;
        ```

# 3.Google Cloud Platform Setup:
      Go to the [Google Cloud Console](https://console.cloud.google.com/).
       Create a new project or select an existing one.
       Enable the "Gmail API" for your project.
       Navigate to "APIs & Services" -> "Credentials".
       Configure the "OAuth consent screen":
           User Type: External
           Provide App name (e.g., "MailMinder (Dev)"), User support email, Developer contact information.
           Scopes: Ensure you add/verify `openid`, `profile`, `email`, and critically `https://www.googleapis.com/auth/gmail.readonly` (or more permissive if needed later, like `gmail.metadata`).
      

4.  # Configure Backend (`application.properties`):
    *   Navigate to `src/main/resources/application.properties`.
    *   Update the following properties:
        ```properties
        # MySQL
        spring.datasource.url=jdbc:mysql://localhost:3306/mailmind_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
        spring.datasource.username=your_mysql_username # e.g., root
        spring.datasource.password=your_mysql_password # e.g., root or your specific password

        # Google OAuth2
        spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID_HERE
        spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET_HERE
        spring.security.oauth2.client.registration.google.scope=openid,profile,email,https://www.googleapis.com/auth/gmail.readonly
        # redirect-uri is usually derived, but can be set explicitly if needed:
        # spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/login/oauth2/code/google

        # Spring Mail (for future notification feature - configure when ready to test notifications)
        # spring.mail.host=smtp.gmail.com
        # spring.mail.port=587
        # spring.mail.username=your-sending-email@gmail.com
        # spring.mail.password=your-gmail-app-password
        # spring.mail.properties.mail.smtp.auth=true
        # spring.mail.properties.mail.smtp.starttls.enable=true
        ```
    *   **Important:** Replace placeholders with your actual MySQL credentials and Google OAuth credentials.

    Build and Run the Spring Boot Application:**
    *   Open a terminal in the project root directory.
    *   **Using Maven Wrapper (recommended):**
        *   On Windows (PowerShell): `.\mvnw spring-boot:run`
        *   On Windows (Command Prompt): `mvnw spring-boot:run`
        *   On macOS/Linux: `./mvnw spring-boot:run`
    *   The backend will start on `http://localhost:8080`.

# Built With

 # Backend:
    *   Java 17
    *   Spring Boot 3.2.x
    *   Spring Security (for OAuth 2.0 with Google)
    *   Spring Data JPA (with Hibernate)
    *   Maven (Build Tool)
    *   MySQL (Database)
    *   **Google API Client Libraries for Java (Gmail API integrated)**
    *   Spring Mail (for notifications - planned)
  # Frontend:
    *   HTML5
    *   CSS3
    *   Vanilla JavaScript (for DOM manipulation and API calls)
    *   Font Awesome (for icons)
    *   Google Fonts

# API Endpoints (Current)

*   `GET /api/emails`: Fetches emails from the authenticated user's Gmail account and enriches them with MailMinder metadata.
*   `POST /api/emails/{gmailMessageId}/set-schedule-priority`: Sets reminder date/time, priority, and notes for an email.
*   `POST /api/emails/{gmailMessageId}/priority`: Updates only the priority for an email.
*   `GET /api/reminders/upcoming`: Fetches upcoming reminders for the user (from MailMinder's database).
*   _(More endpoints for reminder management and profile will be added)_

# Screenshots 
*   Login Page
*   Dashboard View (showing real emails)
*   Scheduler Modal

# Future Enhancements
*   Implement more advanced filtering and searching for emails within the dashboard.
*   More robust error handling and user feedback on the frontend.
*   Unit and integration tests.
*   Deployment to a cloud platform.
