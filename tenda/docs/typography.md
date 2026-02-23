# Application Typography Guide

This document provides a detailed summary of the typography used in the Tenda & Orun application.

---

### 1. Font Families

The application uses two primary font families, loaded via `next/font/google` in the `app/layout.tsx` file:

-   **Primary Sans-Serif Font**: **Geist**
    -   This is the main font used for all standard UI text, including headings, paragraphs, and button labels.
    -   It is applied globally via the `--font-geist-sans` CSS variable.

-   **Primary Monospace Font**: **Geist Mono**
    -   This font is used specifically for displaying code, JSON, and other technical information where character alignment is important (e.g., process variables, instance IDs).
    -   It is applied globally via the `--font-geist-mono` CSS variable and invoked using the `font-mono` Tailwind class.

The `antialiased` Tailwind class is also applied to the `<body>` tag for smoother font rendering across the application.

---

### 2. Font Sizes and Weights

The project uses the **default Tailwind CSS typography scale**. There are no custom font sizes defined in `tailwind.config.ts`. The specific sizes and weights are applied contextually throughout the application's components.

Here is a breakdown of how different text elements are styled:

#### **Headings & Titles:**

-   **Page Headers** (e.g., "Process List", "Task List"):
    -   `text-2xl` (1.5rem / 24px) or `text-3xl` (1.875rem / 30px)
    -   `font-semibold` or `font-bold`

-   **Card/Section Titles** (e.g., "Start a New Process Instance"):
    -   `text-lg` (1.125rem / 18px) or `text-xl` (1.25rem / 20px)
    -   `font-semibold`

#### **Body & Standard Text:**

-   **Paragraphs & Descriptions**:
    -   `text-sm` (0.875rem / 14px) or `text-base` (1rem / 16px) - `text-sm` is more common.
    -   `text-gray-600` is frequently used for secondary text.
    -   `font-medium` for navigation links, `font-normal` (default) for body copy.

-   **Input Fields & Placeholders**:
    -   `text-sm` (0.875rem / 14px)
    -   `placeholder:text-gray-500`

#### **Specialized Text:**

-   **Buttons**:
    -   `text-sm` (0.875rem / 14px) or `text-base` (1rem / 16px)
    -   `font-medium` or `font-bold`

-   **Badges & Tags** (e.g., process version, task status):
    -   `text-sm` (0.875rem / 14px)
    -   `font-semibold`

-   **Code/JSON Blocks**:
    -   `font-mono` (using Geist Mono)
    -   `text-sm` (0.875rem / 14px)

---

### Summary Table

| Element Type                | Font Family   | Common Sizes (Tailwind Class) | Common Weights (Tailwind Class) |
| --------------------------- | ------------- | ----------------------------- | ------------------------------- |
| Page Titles                 | Geist         | `text-2xl`, `text-3xl`        | `font-semibold`, `font-bold`    |
| Section/Card Titles         | Geist         | `text-lg`, `text-xl`          | `font-semibold`                 |
| Body Text / Paragraphs      | Geist         | `text-sm`, `text-base`        | `font-normal` (default)         |
| Navigation Links            | Geist         | `text-sm`                     | `font-medium`                   |
| Buttons                     | Geist         | `text-sm`, `text-base`        | `font-medium`, `font-bold`      |
| Input Fields                | Geist         | `text-sm`                     | `font-normal` (default)         |
| Code, JSON, IDs             | Geist Mono    | `text-sm`                     | `font-normal` (default)         |

This typography system creates a clean, modern, and readable interface with a clear visual hierarchy.
