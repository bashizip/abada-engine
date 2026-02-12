import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { TaskStatus } from "./api";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDistanceToNow(dateString?: string): string {
  if (!dateString) {
    return '-';
  }

  const date = new Date(dateString);
  const now = new Date();
  const seconds = Math.abs(Math.round((now.getTime() - date.getTime()) / 1000));

  if (seconds < 60) {
    return `${seconds} seconds ago`;
  }

  const minutes = Math.round(seconds / 60);
  if (minutes < 60) {
    return `${minutes} minutes ago`;
  }

  const hours = Math.round(minutes / 60);
  if (hours < 24) {
    return `${hours} hours ago`;
  }

  const days = Math.round(hours / 24);
  return `${days} days ago`;
}

export function getTaskStatusColors(status: TaskStatus): string {
  switch (status) {
    case "AVAILABLE":
      return "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300";
    case "CLAIMED":
      return "bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300";
    case "COMPLETED":
      return "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300";
    case "FAILED":
      return "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300";
    default:
      return "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300";
  }
}
