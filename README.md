# Lost & Found Management System

A robust, JavaFX-based application designed to streamline the process of reporting lost items and matching them with found entries. The system employs intelligent algorithms to ensure high qualification standards and accurate similarity-based searching.

## ✨ Key Features

- **Automated Item Qualification**: Built-in logic to filter entries based on tangible value, location, and non-perishability.
- **Intelligent Search**: Utilizes **Jaccard Similarity** scoring to match lost item descriptions with found entries, complete with configurable thresholds.
- **Persistent Storage**: Data is stored reliably in a file-based system with automatic backup and audit logging.
- **Modern GUI**: A polished JavaFX interface with real-time statistics, colorful status indicators, and intuitive navigation.
- **Detailed Audit Log**: Tracks every major action (submissions, removals, searches) for accountability.

## 🚀 Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven 3.6+**

### Installation
1. Clone the repository:
   ```bash
   git clone <repository-url>
   ```
2. Navigate to the project directory:
   ```bash
   cd GUI
   ```
3. Build the project using Maven:
   ```bash
   mvn clean install
   ```

### Running the Application
To launch the GUI:
```bash
mvn javafx:run
```

## 🛠 Technology Stack
- **Language**: Java 17
- **UI Framework**: JavaFX 17
- **Build Tool**: Maven
- **Algorithm**: Jaccard Similarity (for text-based matching)

## 📊 System Overview
- **Item Qualification**: Items must meet specific criteria (location, value, tangibility) before being accepted.
- **Search Process**: Textual descriptions are preprocessed into tokens (removing stopwords and normalization) before calculating similarity.
- **Maintenance**: Admins can remove items and view real-time statistics on active vs. pending items.
"# Smart-Lost-and-Found-System" 
