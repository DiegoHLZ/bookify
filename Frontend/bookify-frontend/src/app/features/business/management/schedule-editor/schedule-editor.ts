import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { ApiError } from '../../../../core/auth/auth.models';
import {
  BookableResource,
  ScheduleException,
  ScheduleExceptionType,
  ScheduleRuleRequest,
  ScheduleRuleType,
  WEEK_DAYS,
  WeekDay,
} from '../../../../core/business/business.models';
import { BusinessService } from '../../../../core/business/business.service';

interface ScheduleRuleDraft extends ScheduleRuleRequest {
  key: number;
}

@Component({
  selector: 'app-schedule-editor',
  imports: [ReactiveFormsModule],
  templateUrl: './schedule-editor.html',
  styleUrl: './schedule-editor.css',
})
export class ScheduleEditor implements OnInit {
  private readonly businessService = inject(BusinessService);
  private nextRuleKey = 1;

  @Input({ required: true }) businessId!: number;
  @Input({ required: true }) resource!: BookableResource;
  @Input() canManage = false;
  @Output() readonly closed = new EventEmitter<void>();

  readonly weekDays = WEEK_DAYS;
  readonly tab = signal<'weekly' | 'exceptions'>('weekly');
  readonly timezone = signal('');
  readonly rules = signal<ScheduleRuleDraft[]>([]);
  readonly exceptions = signal<ScheduleException[]>([]);
  readonly loading = signal(true);
  readonly savingSchedule = signal(false);
  readonly savingException = signal(false);
  readonly deletingDate = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly today = this.localDate(new Date());
  readonly rangeEnd = this.localDate(this.addDays(new Date(), 365));

  readonly exceptionForm = new FormGroup({
    date: new FormControl(this.today, { nonNullable: true, validators: [Validators.required] }),
    exceptionType: new FormControl<ScheduleExceptionType>('CLOSED', { nonNullable: true, validators: [Validators.required] }),
    startTime: new FormControl('09:00', { nonNullable: true }),
    endTime: new FormControl('13:00', { nonNullable: true }),
    reason: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(250)] }),
  });

  ngOnInit(): void {
    forkJoin({
      schedule: this.businessService.getResourceSchedule(this.businessId, this.resource.locationId, this.resource.id),
      exceptions: this.businessService.listScheduleExceptions(
        this.businessId, this.resource.locationId, this.resource.id, this.today, this.rangeEnd,
      ),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ schedule, exceptions }) => {
        this.timezone.set(schedule.timezone);
        this.rules.set(schedule.rules.map((rule) => ({
          key: this.nextRuleKey++,
          dayOfWeek: rule.dayOfWeek,
          ruleType: rule.ruleType,
          startTime: this.shortTime(rule.startTime),
          endTime: this.shortTime(rule.endTime),
        })));
        this.exceptions.set(exceptions);
      },
      error: (error: HttpErrorResponse) => this.error.set(this.readError(error, 'No pudimos cargar los horarios del recurso.')),
    });
  }

  showTab(tab: 'weekly' | 'exceptions'): void {
    this.tab.set(tab);
    this.clearMessages();
  }

  rulesFor(day: WeekDay, type: ScheduleRuleType): ScheduleRuleDraft[] {
    return this.rules().filter((rule) => rule.dayOfWeek === day && rule.ruleType === type);
  }

  addRule(dayOfWeek: WeekDay, ruleType: ScheduleRuleType): void {
    if (!this.canManage) return;
    const defaults = ruleType === 'AVAILABLE'
      ? { startTime: '09:00', endTime: '18:00' }
      : { startTime: '13:00', endTime: '14:00' };
    this.rules.update((rules) => [...rules, { key: this.nextRuleKey++, dayOfWeek, ruleType, ...defaults }]);
    this.clearMessages();
  }

  updateRule(key: number, field: 'startTime' | 'endTime', value: string): void {
    this.rules.update((rules) => rules.map((rule) => rule.key === key ? { ...rule, [field]: value } : rule));
    this.clearMessages();
  }

  removeRule(key: number): void {
    if (!this.canManage) return;
    this.rules.update((rules) => rules.filter((rule) => rule.key !== key));
    this.clearMessages();
  }

  saveWeeklySchedule(): void {
    if (!this.canManage || this.savingSchedule()) return;
    const validationError = this.validateSchedule();
    if (validationError) {
      this.error.set(validationError);
      this.success.set(null);
      return;
    }
    const dayOrder = new Map(this.weekDays.map((day, index) => [day.code, index]));
    const rules: ScheduleRuleRequest[] = this.rules()
      .map(({ dayOfWeek, ruleType, startTime, endTime }) => ({ dayOfWeek, ruleType, startTime, endTime }))
      .sort((left, right) =>
        (dayOrder.get(left.dayOfWeek) ?? 0) - (dayOrder.get(right.dayOfWeek) ?? 0)
        || left.startTime.localeCompare(right.startTime)
        || left.ruleType.localeCompare(right.ruleType),
      );
    this.savingSchedule.set(true);
    this.clearMessages();
    this.businessService.replaceResourceSchedule(
      this.businessId, this.resource.locationId, this.resource.id, rules,
    ).pipe(finalize(() => this.savingSchedule.set(false))).subscribe({
      next: (schedule) => {
        this.timezone.set(schedule.timezone);
        this.rules.set(schedule.rules.map((rule) => ({ ...rule, key: this.nextRuleKey++, startTime: this.shortTime(rule.startTime), endTime: this.shortTime(rule.endTime) })));
        this.success.set('Horario semanal guardado correctamente.');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.readError(error, 'No pudimos guardar el horario semanal.')),
    });
  }

  saveException(): void {
    if (!this.canManage || this.exceptionForm.invalid || this.savingException()) {
      this.exceptionForm.markAllAsTouched();
      return;
    }
    const value = this.exceptionForm.getRawValue();
    if (value.date < this.today || value.date > this.rangeEnd) {
      this.error.set('La fecha debe estar dentro de los próximos 365 días.');
      return;
    }
    if (value.exceptionType === 'CUSTOM_HOURS' && (!value.startTime || !value.endTime || value.startTime >= value.endTime)) {
      this.error.set('El horario especial debe tener una hora de inicio anterior a la hora de fin.');
      return;
    }
    this.savingException.set(true);
    this.clearMessages();
    this.businessService.upsertScheduleException(
      this.businessId, this.resource.locationId, this.resource.id, value.date,
      {
        exceptionType: value.exceptionType,
        startTime: value.exceptionType === 'CUSTOM_HOURS' ? value.startTime : null,
        endTime: value.exceptionType === 'CUSTOM_HOURS' ? value.endTime : null,
        reason: value.reason.trim() || null,
      },
    ).pipe(finalize(() => this.savingException.set(false))).subscribe({
      next: (saved) => {
        this.exceptions.update((items) => [...items.filter((item) => item.exceptionDate !== saved.exceptionDate), saved]
          .sort((left, right) => left.exceptionDate.localeCompare(right.exceptionDate)));
        this.resetExceptionForm();
        this.success.set('Excepción guardada correctamente.');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.readError(error, 'No pudimos guardar la excepción.')),
    });
  }

  editException(exception: ScheduleException): void {
    this.exceptionForm.setValue({
      date: exception.exceptionDate,
      exceptionType: exception.exceptionType,
      startTime: this.shortTime(exception.startTime ?? '09:00'),
      endTime: this.shortTime(exception.endTime ?? '13:00'),
      reason: exception.reason ?? '',
    });
    this.clearMessages();
  }

  deleteException(exception: ScheduleException): void {
    if (!this.canManage || this.deletingDate() !== null) return;
    this.deletingDate.set(exception.exceptionDate);
    this.clearMessages();
    this.businessService.deleteScheduleException(
      this.businessId, this.resource.locationId, this.resource.id, exception.exceptionDate,
    ).pipe(finalize(() => this.deletingDate.set(null))).subscribe({
      next: () => {
        this.exceptions.update((items) => items.filter((item) => item.exceptionDate !== exception.exceptionDate));
        this.success.set('Excepción eliminada.');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.readError(error, 'No pudimos eliminar la excepción.')),
    });
  }

  exceptionTypeName(type: ScheduleExceptionType): string {
    return type === 'CLOSED' ? 'Día cerrado' : 'Horario especial';
  }

  formatDate(date: string): string {
    return new Intl.DateTimeFormat('es-PE', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' })
      .format(new Date(`${date}T00:00:00Z`));
  }

  requestClose(): void {
    if (!this.savingSchedule() && !this.savingException() && this.deletingDate() === null) this.closed.emit();
  }

  private validateSchedule(): string | null {
    for (const day of this.weekDays) {
      const available = this.rulesFor(day.code, 'AVAILABLE').sort((a, b) => a.startTime.localeCompare(b.startTime));
      const breaks = this.rulesFor(day.code, 'BREAK').sort((a, b) => a.startTime.localeCompare(b.startTime));
      for (const rule of [...available, ...breaks]) {
        if (!rule.startTime || !rule.endTime || rule.startTime >= rule.endTime) {
          return `${day.name}: cada intervalo debe comenzar antes de terminar.`;
        }
      }
      if (this.hasOverlap(available)) return `${day.name}: los intervalos disponibles no pueden superponerse.`;
      if (this.hasOverlap(breaks)) return `${day.name}: los descansos no pueden superponerse.`;
      for (const pause of breaks) {
        if (!available.some((interval) => interval.startTime <= pause.startTime && pause.endTime <= interval.endTime)) {
          return `${day.name}: cada descanso debe estar dentro de un intervalo disponible.`;
        }
      }
    }
    return null;
  }

  private hasOverlap(rules: ScheduleRuleDraft[]): boolean {
    return rules.some((rule, index) => index > 0 && rule.startTime < rules[index - 1]!.endTime);
  }

  private resetExceptionForm(): void {
    this.exceptionForm.reset({ date: this.today, exceptionType: 'CLOSED', startTime: '09:00', endTime: '13:00', reason: '' });
  }

  private clearMessages(): void {
    this.error.set(null);
    this.success.set(null);
  }

  shortTime(value: string): string {
    return value.slice(0, 5);
  }

  private addDays(date: Date, days: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  }

  private localDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private readError(error: HttpErrorResponse, fallback: string): string {
    const apiError = error.error as Partial<ApiError> | null;
    if (apiError?.validationErrors) return Object.values(apiError.validationErrors)[0] ?? fallback;
    return apiError?.message ?? fallback;
  }
}
