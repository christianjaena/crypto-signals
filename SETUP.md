# Environment Setup

## MEXC API Configuration

This application uses MEXC API for fetching cryptocurrency data. You need to configure your API credentials using environment variables.

### Step 1: Get MEXC API Keys

1. Go to [MEXC](https://mexc.com) and log in to your account
2. Navigate to API Management
3. Create a new API key
4. Note down your Access Key and Secret Key

### Step 2: Set Environment Variables

#### Option A: Create a `.env` file (for development)

1. Copy the example file:
   ```bash
   cp .env.example .env
   ```

2. Edit the `.env` file with your actual API keys:
   ```env
   MEXC_ACCESS_KEY=your_mexc_access_key_here
   MEXC_SECRET_KEY=your_mexc_secret_key_here
   ```

#### Option B: Set environment variables directly

**Windows (Command Prompt):**
```cmd
set MEXC_ACCESS_KEY=your_mexc_access_key_here
set MEXC_SECRET_KEY=your_mexc_secret_key_here
```

**Windows (PowerShell):**
```powershell
$env:MEXC_ACCESS_KEY="your_mexc_access_key_here"
$env:MEXC_SECRET_KEY="your_mexc_secret_key_here"
```

**Linux/Mac:**
```bash
export MEXC_ACCESS_KEY=your_mexc_access_key_here
export MEXC_SECRET_KEY=your_mexc_secret_key_here
```

#### Option C: IDE Configuration

Most IDEs allow you to set environment variables in the run configuration:

- **IntelliJ IDEA**: Run/Debug Configurations → Application → Environment variables
- **VS Code**: launch.json configuration → env property
- **Eclipse**: Run Configurations → Environment tab

### Step 3: Verify Configuration

The application will use the environment variables if set, or fall back to default values if not configured. Make sure your `.env` file is never committed to version control (it's already included in `.gitignore`).

### Security Notes

- Never commit your actual API keys to version control
- Use different API keys for development and production
- Regularly rotate your API keys
- Limit API key permissions to only what's necessary (read-only for this application)
