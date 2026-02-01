import { Component, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { SpaceService } from '../../services/space.service';
import { FormsModule } from '@angular/forms';
import { FullCalendarComponent } from '@fullcalendar/angular';

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule, FullCalendarModule, FormsModule],
  templateUrl: './calendar.html'
})
export class CalendarComponent implements OnChanges {
  @Input() spaces: any[] = [];
  @ViewChild('managerCalendar') calendar?: FullCalendarComponent;

  selectedSpaceId = '';
  selectedElementType: 'open' | 'office' | 'conference' = 'open';
  selectedElementName = '';
  elementOptions: string[] = [];

  calendarOptions: CalendarOptions = {
    initialView: 'timeGridWeek',
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
    events: (info, successCallback, failureCallback) => {
      this.fetchEvents(info.startStr, info.endStr, successCallback, failureCallback);
    },
    editable: true,
    eventClick: this.handleEventClick.bind(this),
    eventDrop: this.handleEventDrop.bind(this),
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'timeGridWeek,dayGridMonth'
    },
    allDaySlot: false
  };

  constructor(private spaceService: SpaceService) { }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['spaces'] && this.spaces.length && !this.selectedSpaceId) {
      this.selectedSpaceId = this.spaces[0].id;
      this.updateElementOptions();
      this.refetchEvents();
    }
  }

  handleEventClick(arg: any) {
    const data = arg.event.extendedProps.fullData;
    alert(`Space: ${data.spaceName}\nType: ${data.type}\nItem: ${data.itemName || 'N/A'}\nStatus: ${data.status}\nFrom: ${new Date(data.startDateTime).toLocaleString()}`);
  }

  handleEventDrop(arg: any) {
    const data = arg.event.extendedProps.fullData;
    if (data.type === 'open') {
      arg.revert();
      alert('Open-space reservations cannot be rescheduled here.');
      return;
    }

    const startDateTime = arg.event.start?.toISOString();
    const endDateTime = arg.event.end?.toISOString();

    if (!startDateTime || !endDateTime) {
      arg.revert();
      return;
    }

    this.spaceService.rescheduleReservation(data.id, startDateTime, endDateTime).subscribe({
      next: () => this.refetchEvents(),
      error: () => {
        arg.revert();
        alert('Reschedule failed');
      }
    });
  }

  onSelectionChange() {
    this.updateElementOptions();
    this.refetchEvents();
  }

  private updateElementOptions() {
    const selectedSpace = this.spaces.find(s => s.id === this.selectedSpaceId);
    if (!selectedSpace) return;

    if (this.selectedElementType === 'office') {
      this.elementOptions = (selectedSpace.offices || []).map((o: any) => o.name);
    } else if (this.selectedElementType === 'conference') {
      this.elementOptions = (selectedSpace.conferenceRooms || []).map((c: any) => c.name);
    } else {
      this.elementOptions = [];
      this.selectedElementName = '';
      return;
    }

    this.selectedElementName = this.elementOptions[0] || '';
  }

  fetchEvents(start: string, end: string, successCallback: (events: any[]) => void, failureCallback: (err: any) => void) {
    if (!this.selectedSpaceId) {
      successCallback([]);
      return;
    }

    this.spaceService.getManagerCalendarEvents(
      this.selectedSpaceId,
      this.selectedElementType,
      this.selectedElementName || null,
      start,
      end
    ).subscribe({
      next: (events) => successCallback(events),
      error: (err) => failureCallback(err)
    });
  }

  private refetchEvents() {
    const api = this.calendar?.getApi();
    if (!api) return;
    api.refetchEvents();
  }
}
