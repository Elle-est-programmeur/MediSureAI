import React, { useEffect, useState } from 'react';
import { getTimeline } from '../services/api';
import './Timeline.css';

const Timeline = () => {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchTimeline = async () => {
            try {
                const data = await getTimeline();
                setEvents(data);
            } catch (err) {
                console.error("Failed to fetch timeline", err);
            } finally {
                setLoading(false);
            }
        };
        fetchTimeline();
    }, []);

    if (loading) return <div className="timeline-loading">Scanning records...</div>;

    if (events.length === 0) {
        return (
            <div className="timeline-empty">
                <div className="empty-icon">📅</div>
                <p>No dated records found.</p>
                <span>Upload medical reports with dates to populate your timeline.</span>
            </div>
        );
    }

    return (
        <div className="timeline-container">
            <h3 className="timeline-title">Health Journey</h3>
            <div className="timeline-list">
                {events.map((event, index) => (
                    <div key={event.id} className="timeline-item">
                        <div className="timeline-marker">
                            <div className="marker-dot"></div>
                            {index !== events.length - 1 && <div className="marker-line"></div>}
                        </div>
                        <div className="timeline-content">
                            <span className="event-date">
                                {new Date(event.eventDate).toLocaleDateString('en-US', {
                                    year: 'numeric',
                                    month: 'short',
                                    day: 'numeric'
                                })}
                            </span>
                            <div className="event-card">
                                <div className="event-type-icon">
                                    {event.documentType === 'MEDICAL_REPORT' ? '📄' : 
                                     event.documentType === 'PRESCRIPTION' ? '💊' : 
                                     event.documentType === 'LAB_RESULT' ? '🧪' : '📝'}
                                </div>
                                <div className="event-info">
                                    <span className="event-name">{event.fileName}</span>
                                    <span className="event-type">{event.documentType.replace('_', ' ')}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default Timeline;
