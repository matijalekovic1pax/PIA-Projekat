import { Component, Input, OnInit, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { SpaceService } from '../../services/space.service';
import { CommonModule } from '@angular/common';
import { FullCalendarModule, FullCalendarComponent } from '@fullcalendar/angular';
import { CalendarOptions } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';

@Component({
  selector: 'app-reservation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FullCalendarModule],
  templateUrl: './reservation.html'
})
export class ReservationComponent implements OnInit, OnChanges {
  @Input() space: any;
  @ViewChild('calendar') calendar?: FullCalendarComponent;
  reservationForm: FormGroup;
  availability: any = null;
  message: string = '';
  minDate: string;
  timeError = '';
  currentItemIndex = 0;
  calendarOptions: CalendarOptions = {
    initialView: 'timeGridWeek',
    plugins: [dayGridPlugin, timeGridPlugin],
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'timeGridWeek,dayGridMonth'
    },
    allDaySlot: false,
    events: (info, successCallback, failureCallback) => {
      this.loadCalendarEvents(info.startStr, info.endStr, successCallback, failureCallback);
    }
  };

  constructor(private fb: FormBuilder, private spaceService: SpaceService) {
    this.minDate = new Date().toISOString().split('T')[0];
    this.reservationForm = this.fb.group({
      date: [this.minDate, Validators.required],
      startTime: ['09:00', Validators.required],
      endTime: ['17:00', Validators.required],
      type: ['open', Validators.required],
      itemId: [''] // specific office or conf room name
    });
  }

  ngOnInit() {
    if (this.space) {
      this.checkAvailability();
    }

    this.reservationForm.valueChanges.subscribe(() => {
      // Debounce or just check on change if valid
      if (this.reservationForm.get('date')?.valid && this.reservationForm.get('type')?.valid) {
        this.checkAvailability();
      }
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['space'] && !changes['space'].firstChange) {
      this.checkAvailability();
    }
  }

  checkAvailability() {
    const { date, type, startTime, endTime } = this.reservationForm.value;
    if (!date || !type || !this.space || !startTime || !endTime) return;

    if (!this.isTimeRangeValid(startTime, endTime)) {
      this.timeError = 'End time must be after start time.';
      this.availability = null;
      return;
    }
    this.timeError = '';

    this.spaceService.getAvailability(this.space.id, date, type, startTime, endTime).subscribe({
      next: (data) => {
        this.availability = data;
        // Reset itemId if type changed or not applicable
        if (type === 'open') {
          this.reservationForm.get('itemId')?.clearValidators();
          this.reservationForm.get('itemId')?.setValue('');
        } else {
          this.reservationForm.get('itemId')?.setValidators(Validators.required);
          this.currentItemIndex = 0;
          const items = type === 'office' ? data.offices : data.conferenceRooms;
          if (items?.length) {
            this.reservationForm.patchValue({ itemId: items[0].name }, { emitEvent: false });
          }
        }
        this.reservationForm.get('itemId')?.updateValueAndValidity({ emitEvent: false });
        this.refetchCalendar();
      },
      error: (err) => console.error(err)
    });
  }

  selectItem(name: string) {
    this.reservationForm.patchValue({ itemId: name });
    this.refetchCalendar();
  }

  onSubmit() {
    if (this.reservationForm.valid) {
      const { date, type, itemId, startTime, endTime } = this.reservationForm.value;
      if (!this.isTimeRangeValid(startTime, endTime)) {
        this.message = 'End time must be after start time.';
        return;
      }

      const startDateTime = `${date}T${startTime}:00`;
      const endDateTime = `${date}T${endTime}:00`;
      const data = {
        spaceId: this.space.id,
        startDateTime,
        endDateTime,
        type,
        itemId
      };

      this.spaceService.createReservation(data).subscribe({
        next: () => {
          this.message = 'Reservation confirmed!';
          this.checkAvailability(); // Refresh
          this.availability = null;
          setTimeout(() => this.message = '', 3000);
        },
        error: (err) => this.message = err.error?.message || 'Reservation failed'
      });
    }
  }

  previousItem() {
    if (!this.availability) return;
    const { type } = this.reservationForm.value;
    const items = type === 'office' ? this.availability.offices : this.availability.conferenceRooms;
    if (!items?.length) return;
    this.currentItemIndex = (this.currentItemIndex - 1 + items.length) % items.length;
    this.selectItem(items[this.currentItemIndex].name);
  }

  nextItem() {
    if (!this.availability) return;
    const { type } = this.reservationForm.value;
    const items = type === 'office' ? this.availability.offices : this.availability.conferenceRooms;
    if (!items?.length) return;
    this.currentItemIndex = (this.currentItemIndex + 1) % items.length;
    this.selectItem(items[this.currentItemIndex].name);
  }

  private isTimeRangeValid(startTime: string, endTime: string): boolean {
    return startTime < endTime;
  }

  private loadCalendarEvents(
    start: string,
    end: string,
    successCallback: (events: any[]) => void,
    failureCallback: (error: any) => void
  ) {
    if (!this.space) {
      successCallback([]);
      return;
    }

    const { type, itemId } = this.reservationForm.value;
    this.spaceService.getCalendarEvents(this.space.id, type, itemId, start, end).subscribe({
      next: (events) => successCallback(events),
      error: (err) => failureCallback(err)
    });
  }

  private refetchCalendar() {
    const api = this.calendar?.getApi();
    if (api) {
      api.refetchEvents();
    }
  }
}

