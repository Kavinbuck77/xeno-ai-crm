# XENO AI CRM

An AI-Native Mini CRM platform built as part of the Xeno Engineering Take-Home Assignment.

## Live Demo

Frontend:
https://xeno-ai-iopgbb7r7-kkks-projects-4cb723de.vercel.app

Backend:
https://xeno-crm-service.onrender.com

Channel Service:
https://xeno-channel-service-499m.onrender.com

---

## Overview

XENO AI CRM helps brands intelligently reach their shoppers through AI-powered marketing campaigns.

The platform enables marketers to:

- Manage customer data
- Generate AI-powered campaign recommendations
- Create and launch campaigns
- Simulate communication delivery through a separate channel service
- Track campaign delivery performance in real time

---

## Features

### Customer Management

- Add customer profiles
- Store customer details
- View customer database

### AI Campaign Studio

- Enter marketing goals in natural language
- AI generates:
  - Audience Segment
  - Recommended Channel
  - Personalized Message

### Campaign Management

- Save campaigns
- Launch campaigns
- Monitor campaign status

### Analytics Dashboard

- Total Customers
- Total Campaigns
- Messages Sent
- Delivery Rate

### Channel Service Simulation

A separate microservice simulates:

- Message delivery
- Delivery callbacks
- Communication lifecycle tracking

---

## Architecture

Frontend (React + Vite)
|
v
CRM Backend (Spring Boot)
|
v
Channel Service (Spring Boot)
|
v
Receipt Callback API
|
v
Communication Analytics

---

## Tech Stack

### Frontend

- React
- Vite
- Axios
- CSS

### Backend

- Java 21
- Spring Boot
- Spring Data JPA
- H2 Database

### AI

- Google Gemini API

### Deployment

- Vercel (Frontend)
- Render (CRM Backend)
- Render (Channel Service)

---

## AI-Native Approach

The platform integrates AI directly into campaign creation.

A marketer only provides a business goal such as:

"Bring back inactive customers"

The AI automatically recommends:

- Target Segment
- Communication Channel
- Marketing Message

This reduces campaign planning effort and helps marketers execute campaigns faster.

---

## Communication Flow

1. User generates an AI campaign.
2. Campaign is saved in CRM.
3. User launches campaign.
4. CRM sends communication requests to Channel Service.
5. Channel Service simulates delivery.
6. Channel Service sends delivery receipt callback.
7. CRM updates communication status.
8. Dashboard analytics update automatically.

---

## Project Structure

xeno-ai-crm

├── frontend (React)

├── crm-service (Spring Boot)

└── channel-service (Spring Boot)

---

## Future Improvements

- Advanced customer segmentation
- Open / Click tracking
- Multi-channel delivery
- Retry mechanisms
- Queue-based processing
- PostgreSQL integration
- Agentic AI campaign execution

---

## Author

Kavin K K

SRM Institute of Science and Technology

Registration Number: RA2311003020070
