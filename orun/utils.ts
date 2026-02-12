/**
 * Utility functions for formatting dates, times, and durations
 */

/**
 * Calculate the duration between a start time and now (or end time if provided)
 * @param startTime ISO string of start time
 * @param endTime Optional ISO string of end time
 * @returns Formatted duration string (e.g., "2h 30m", "45m", "3d 5h")
 */
export function calculateDuration(startTime: string, endTime?: string): string {
    const start = new Date(startTime).getTime();
    const end = endTime ? new Date(endTime).getTime() : Date.now();
    const diffMs = end - start;

    const minutes = Math.floor(diffMs / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) {
        const remainingHours = hours % 24;
        return remainingHours > 0 ? `${days}d ${remainingHours}h` : `${days}d`;
    }

    if (hours > 0) {
        const remainingMinutes = minutes % 60;
        return remainingMinutes > 0 ? `${hours}h ${remainingMinutes}m` : `${hours}h`;
    }

    return `${minutes}m`;
}

/**
 * Calculate relative time from a given date to now
 * @param dateTime ISO string of the date/time
 * @returns Relative time string (e.g., "2h ago", "3d ago")
 */
export function getRelativeTime(dateTime: string): string {
    const diff = Date.now() - new Date(dateTime).getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) return `${days}d ago`;
    if (hours > 0) return `${hours}h ago`;
    if (minutes > 0) return `${minutes}m ago`;
    return 'just now';
}

/**
 * Format a date/time for display
 * @param dateTime ISO string of the date/time
 * @returns Object with formatted date and time strings
 */
export function formatDateTime(dateTime: string): { date: string; time: string; full: string } {
    const dt = new Date(dateTime);
    return {
        date: dt.toLocaleDateString(),
        time: dt.toLocaleTimeString(),
        full: dt.toLocaleString()
    };
}
