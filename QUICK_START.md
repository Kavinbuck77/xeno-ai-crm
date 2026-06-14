# 🚀 Gemini AI Integration - Quick Reference

## One-Time Setup (5 min)

```bash
# 1. Get Free API Key
https://aistudio.google.com/app/apikey

# 2. Set Environment Variable
$env:GEMINI_API_KEY = "paste-your-key-here"

# That's it! ✅
```

## Running the App

### Terminal 1 - Backend

```bash
cd xeno-crm-backend/crm-service
mvnw.cmd clean spring-boot:run
# Runs on http://localhost:8080
```

### Terminal 2 - Frontend

```bash
cd xeno-crm-frontend
npm install        # First time only
npm run dev
# Runs on http://localhost:5173
```

## Testing

1. Open: http://localhost:5173
2. Go to: Campaign Studio
3. Try: "Bring back inactive customers"
4. Click: "Generate AI Campaign"
5. See: Real AI magic! ✨

## Example Inputs

- "Increase sales for new customers"
- "Create holiday promotion campaign"
- "Reactivate dormant accounts"
- "Promote premium tier membership"

## What You Get

```json
{
  "segment": "Smart audience targeting",
  "channel": "Email | WhatsApp | SMS | Push",
  "message": "AI-written marketing copy",
  "name": "Your campaign name"
}
```

## If Something Breaks

| Problem                 | Fix                                     |
| ----------------------- | --------------------------------------- |
| Can't generate campaign | Check `GEMINI_API_KEY` is set           |
| Build fails             | Run `mvnw clean test-compile`           |
| Port already in use     | Change port in `application.properties` |
| Timeout                 | Check internet connection               |

## Files Changed

- ✏️ `pom.xml` - Added Gson dependency
- ✏️ `application.properties` - Added API config
- ✏️ `AiCampaignService.java` - Real Gemini integration

## Key Endpoints

```
POST http://localhost:8080/ai/generate-campaign
{
  "goal": "Your marketing goal here"
}
```

## Important

⚠️ **Security**:

- Never commit API key to git
- Always use environment variable
- Rotate key periodically

## Need Help?

See detailed guide: [GEMINI_SETUP.md](./GEMINI_SETUP.md)

---

**Happy Campaigning! 🎉**
