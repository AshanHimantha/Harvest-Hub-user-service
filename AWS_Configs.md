# Harvest Hub - AWS Environment Setup Guide

This document provides the step-by-step instructions to configure the necessary AWS services for the Harvest Hub backend. All services should be created in the `ap-southeast-2` (Sydney) region unless otherwise specified.

---

### Part 1: Configure IAM User for Local Development

This IAM user provides the necessary credentials for the backend services to communicate with AWS from a local development machine.

1.  **Navigate to the IAM service** in the AWS Management Console.
2.  Go to **Users** and click **Create user**.
3.  **User name:** Enter `harvest-hub-dev-user`.
4.  Click **Next**.
5.  On the "Set permissions" page, select **Attach policies directly**.
6.  In the permissions policy search box, type `AmazonCognitoPowerUser` and check the box next to it.
7.  Click **Next**, review the details, and click **Create user**.
8.  **Create Access Key:**
    *   Click on the newly created `harvest-hub-dev-user`.
    *   Go to the **Security credentials** tab.
    *   In the "Access keys" section, click **Create access key**.
    *   Select **Command Line Interface (CLI)**, acknowledge the warning, and click **Next**.
    *   Click **Create access key**.
    *   **Crucial:** Copy the **Access key ID** and **Secret access key** and save them securely. These will be used to configure the local development environment via `aws configure`.

---

### Part 2: Configure Cognito User Pool

This service will manage all user identities, groups (roles), and authentication.

1.  **Navigate to the Cognito service** and click **Create user pool**.
2.  **Step 1: Configure sign-in experience:**
    *   Under "Cognito user pool sign-in options", select **Email**.
    *   Click **Next**.
3.  **Step 2 & 3: Security and Sign-up:**
    *   Leave all settings as the default on these pages.
    *   Click **Next** on both pages.
4.  **Step 4: Configure message delivery:**
    *   Select **Send email with Cognito**.
    *   Click **Next**.
5.  **Step 5: Integrate your app:**
    *   **User pool name:** Enter `Harvest-Hub-Users`.
    *   **App type:** Select **Public client**.
    *   **App client name:** Enter `harvest-hub-frontend-client`.
    *   **IMPORTANT:** Uncheck the box **Generate a client secret**.
    *   Click **Next**, then **Create user pool**.
6.  **Configure App Client Settings:**
    *   After the pool is created, navigate to the **App integration** tab.
    *   Click on the `harvest-hub-frontend-client`.
    *   In the **"Attribute read and write permissions"** section, click **Edit**.
    *   Enable **Read** permissions for: `email`, `email_verified`, `family_name`, and `given_name`. Save changes.
    *   In the **"Hosted UI"** section (or similar settings), click **Edit**.
    *   Under **"Allowed OAuth Scopes"**, ensure the following are checked: `openid`, `email`, and `profile`. Save changes.
7.  **Create User Groups (Roles):**
    *   Navigate to the **Users** tab.
    *   Create the following groups, leaving the "IAM role" field blank:
        *   `SuperAdmins` (Precedence: 1)
        *   `Suppliers` (Precedence: 20)
        *   `DataStewards` (Precedence: 30)
        *   `Customers` (Precedence: 10)
8.  **Customize Email Templates:**
    *   Navigate to the **Messaging** tab.
    *   Under **Message templates**, customize the **Invitation messages** and **Verification messages** with the project name "Harvest Hub" and your domain.

---

### Part 3: Configure Lambda Trigger for Automatic Role Assignment

This function will automatically assign new users to the `Customers` group upon sign-up.

1.  **Navigate to the Lambda service** and click **Create function**.
2.  **Function name:** Enter `HarvestHub-PostConfirmation-Trigger`.
3.  **Runtime:** Select a recent **Node.js** version (e.g., 18.x).
4.  **Permissions:** Choose **Create a new role with basic Lambda permissions**.
5.  **Add IAM Permission:**
    *   After creation, go to the **Configuration -> Permissions** tab and click the **Role name**.
    *   In IAM, click **Add permissions -> Create inline policy**.
    *   Use the JSON editor and paste the following, replacing the `Resource` ARN with your Cognito User Pool's ARN:
        ```json
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Action": "cognito-idp:AdminAddUserToGroup",
                    "Resource": "arn:aws:cognito-idp:ap-southeast-2:ACCOUNT_ID:userpool/USER_POOL_ID"
                }
            ]
        }
        ```
    *   Name the policy and save it.
6.  **Add Function Code:**
    *   Go back to the Lambda function's **Code** tab.
    *   Paste the Node.js code that adds a user to the `Customers` group.
        ```javascript
        import { CognitoIdentityProviderClient, AdminAddUserToGroupCommand } from "@aws-sdk/client-cognito-identity-provider";
        const client = new CognitoIdentityProviderClient({});

        export const handler = async (event) => {
            const groupName = "Customers";
            const params = {
                GroupName: groupName,
                UserPoolId: event.userPoolId,
                Username: event.userName,
            };
            try {
                await client.send(new AdminAddUserToGroupCommand(params));
            } catch (err) {
                console.error(`Error adding user to group: ${err}`);
            }
            return event;
        };
        ```
    *   Click **Deploy**.
7.  **Set the Cognito Trigger:**
    *   Go back to your Cognito User Pool.
    *   Navigate to the **Triggers** settings page.
    *   In the **Post confirmation** trigger section, select the `HarvestHub-PostConfirmation-Trigger` Lambda function.
    *   Save the changes.

---
This guide provides all the necessary steps to provision the AWS backend infrastructure required for the application to run.
