# Mosque Payment API

Spring Boot 3 / Java 17 / MySQL. Collects zakat, sadaqah, Jumu'ah donations, building-fund gifts and membership dues through Paystack.

## Payment flow
1. `POST /payments` - saves a PENDING payment and returns Paystack's `authorizationUrl`. Redirect the payer there.
2. Payer pays on Paystack and is sent back to `paystack.callback-url`.
3. `GET /payments/{reference}/verify` (or Paystack's webhook `POST /payments/webhook`) - we ask Paystack directly whether it was paid and flip the payment to SUCCESSFUL or FAILED.
4. Admin: `GET /payments/admin/list?status=SUCCESSFUL` and `GET /payments/admin/summary` (HTTP Basic).

## Run
```
export PAYSTACK_SECRET_KEY=sk_test_xxx
export DS_USERNAME=root DS_PASSWORD=yourpassword
./mvnw spring-boot:run
```
Create the MySQL database `mosquepayment` first. Set the webhook URL in your Paystack dashboard to `https://<your-host>/payments/webhook`.
