# Gemini 2.5 Flash AI Integration Setup Guide

## Overview

Your Xeno AI CRM has been upgraded from **fake AI** to **real Gemini 2.5 Flash AI**. The campaign generation now uses Google's state-of-the-art AI model for intelligent marketing campaign recommendations.

## What Changed

### Backend Updates ✨

- **AiCampaignService**: Now calls Gemini 2.5 Flash API instead of using hardcoded rules
- **Dependencies Added**: Gson library for JSON parsing
- **Java Version**: Updated to Java 23 (compatible with Java 17+)
- **Configuration**: Added Gemini API key configuration in `application.properties`

### Features

- ✅ Intelligent campaign segment recommendations
- ✅ Smart communication channel selection (Email, WhatsApp, SMS, Push)
- ✅ Compelling marketing message generation
- ✅ Natural language processing for marketing goals

## Setup Instructions

### Step 1: Get Gemini API Key

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Click "Create API Key" or "Get API Key"
3. A new API key will be generated (it's free for development)
4. Copy the API key

### Step 2: Configure the Backend

#### Option A: Environment Variable (Recommended for Production)

```bash
# Windows PowerShell
$env:GEMINI_API_KEY = "your-api-key-here"

# Windows CMD
set GEMINI_API_KEY=your-api-key-here

# Linux/Mac
export GEMINI_API_KEY="your-api-key-here"
```

#### Option B: Direct Configuration (For Local Development)

Edit `xeno-crm-backend/crm-service/src/main/resources/application.properties`:

```properties
# Replace 'your-api-key-here' with your actual API key
gemini.api.key=your-api-key-here
gemini.model.name=gemini-2.5-flash
```

### Step 3: Run the Backend

```bash
cd xeno-crm-backend/crm-service

# On Windows
mvnw.cmd clean spring-boot:run

# On Linux/Mac
./mvnw clean spring-boot:run
```

The service will start on `http://localhost:8080`

### Step 4: Run the Frontend

```bash
cd xeno-crm-frontend

npm install
npm run dev
```

The frontend will start on `http://localhost:5173`

## Testing the Integration

1. Open the frontend at `http://localhost:5173`
2. Navigate to **Campaign Studio**
3. Enter a marketing goal like:
   - "Bring back inactive customers"
   - "Increase sales for high-value products"
   - "Launch a seasonal promotion"
4. Click "Generate AI Campaign"
5. Gemini will generate intelligent recommendations for:
   - **Segment**: Target customer group
   - **Channel**: Best communication method
   - **Message**: Compelling marketing copy

## Project Structure

```
xeno-crm-backend/
  crm-service/
    src/main/java/com/kavin/xeno/crm/
      controller/
        AiController.java        (API endpoint)
      service/
        AiCampaignService.java   (Gemini integration)
    src/main/resources/
      application.properties     (Config & API key)
    pom.xml                      (Updated with Gson dependency)
```

## API Endpoint

**POST** `/ai/generate-campaign`

### Request

```json
{
  "goal": "Bring back inactive customers"
}
```

### Response

```json
{
  "segment": "Customers inactive for 60+ days",
  "channel": "WhatsApp",
  "message": "We miss you! Come back and enjoy exclusive discounts on your favorite products.",
  "name": "AI Campaign: Bring back inactive customers"
}
```

## Troubleshooting

### Issue: "Could not resolve dependencies"

**Solution**: Clear Maven cache and rebuild:

```bash
mvn clean -Dmaven.repo.local=./m2/repository
mvnw clean spring-boot:run
```

### Issue: API Key Not Working

- Verify the key is copied correctly (no extra spaces)
- Check the environment variable is set: `echo %GEMINI_API_KEY%`
- Make sure the API is enabled in Google Cloud Console

### Issue: CORS Errors on Frontend

- The backend has `@CrossOrigin` enabled
- Make sure frontend is calling `http://localhost:8080`
- Check browser console for specific error messages

### Issue: Connection Timeout

- Check your internet connection
- Verify firewall allows outbound HTTPS connections
- The Gemini API should respond within 10-15 seconds

## API Response Fallback

If the Gemini API is unavailable or returns an error, the system will automatically fall back to:

```
Segment: All Customers
Channel: WhatsApp
Message: Check out our latest offers.
```

This ensures the application continues to work even if there are temporary API issues.

## Performance

- **Response Time**: ~2-5 seconds per request (network dependent)
- **Token Limit**: Up to 500 tokens per response (sufficient for campaign generation)
- **Temperature**: Set to 0.7 for balanced creativity and consistency

## Security Considerations

⚠️ **Important**:

- Never commit your API key to version control
- Use environment variables in production
- Rotate your API key periodically
- Monitor API usage in Google Cloud Console

## Next Steps

1. ✅ Set up your Gemini API key
2. ✅ Configure the backend
3. ✅ Start the backend service
4. ✅ Start the frontend
5. ✅ Test the campaign generation
6. ✅ Deploy to production (set env variable securely)

## Support

For issues with:

- **Gemini API**: Check [Google AI Documentation](https://ai.google.dev/docs)
- **Spring Boot**: Check [Spring Boot Docs](https://spring.io/projects/spring-boot)
- **Frontend Issues**: Check React and Vite documentation

## Deployment Checklist

- [ ] API key stored in environment variable (not in code)
- [ ] Backend builds successfully (`mvnw clean test-compile`)
- [ ] Frontend dependencies installed (`npm install`)
- [ ] Campaign generation tested with sample goals
- [ ] Error handling verified (test with invalid API key)
- [ ] Response times acceptable for your use case
- [ ] CORS properly configured

---

**Happy campaigning! 🚀**
