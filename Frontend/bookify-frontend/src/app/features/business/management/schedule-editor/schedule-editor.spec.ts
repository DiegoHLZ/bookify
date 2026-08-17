import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { BusinessService } from '../../../../core/business/business.service';
import { ScheduleEditor } from './schedule-editor';

describe('ScheduleEditor', () => {
  let fixture: ComponentFixture<ScheduleEditor>;
  let component: ScheduleEditor;
  const getResourceSchedule = vi.fn();
  const replaceResourceSchedule = vi.fn();
  const listScheduleExceptions = vi.fn();
  const upsertScheduleException = vi.fn();
  const deleteScheduleException = vi.fn();

  beforeEach(async () => {
    getResourceSchedule.mockReset().mockReturnValue(of({
      businessId: 7, locationId: 3, resourceId: 12, timezone: 'America/Lima',
      rules: [
        { id: 1, dayOfWeek: 'MONDAY', ruleType: 'AVAILABLE', startTime: '09:00:00', endTime: '18:00:00' },
        { id: 2, dayOfWeek: 'MONDAY', ruleType: 'BREAK', startTime: '13:00:00', endTime: '14:00:00' },
      ],
    }));
    replaceResourceSchedule.mockReset();
    listScheduleExceptions.mockReset().mockReturnValue(of([]));
    upsertScheduleException.mockReset();
    deleteScheduleException.mockReset();

    await TestBed.configureTestingModule({
      imports: [ScheduleEditor],
      providers: [{
        provide: BusinessService,
        useValue: { getResourceSchedule, replaceResourceSchedule, listScheduleExceptions, upsertScheduleException, deleteScheduleException },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(ScheduleEditor);
    fixture.componentRef.setInput('businessId', 7);
    fixture.componentRef.setInput('resource', { id: 12, businessId: 7, locationId: 3, name: 'Ana Torres', description: null, type: 'PROFESSIONAL', capacity: 1, active: true, createdAt: '', updatedAt: '' });
    fixture.componentRef.setInput('canManage', true);
    fixture.detectChanges();
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('loads weekly rules, timezone and upcoming exceptions', () => {
    expect(getResourceSchedule).toHaveBeenCalledWith(7, 3, 12);
    expect(listScheduleExceptions).toHaveBeenCalledWith(7, 3, 12, component.today, component.rangeEnd);
    expect(component.timezone()).toBe('America/Lima');
    expect(component.rulesFor('MONDAY', 'AVAILABLE')).toHaveLength(1);
    expect(component.rulesFor('MONDAY', 'BREAK')).toHaveLength(1);
  });

  it('rejects a break outside the available period before calling the API', () => {
    component.addRule('TUESDAY', 'BREAK');

    component.saveWeeklySchedule();

    expect(replaceResourceSchedule).not.toHaveBeenCalled();
    expect(component.error()).toContain('Martes');
  });

  it('saves the complete validated weekly schedule', () => {
    replaceResourceSchedule.mockReturnValue(of({ businessId: 7, locationId: 3, resourceId: 12, timezone: 'America/Lima', rules: [] }));

    component.saveWeeklySchedule();

    expect(replaceResourceSchedule).toHaveBeenCalledWith(7, 3, 12, [
      { dayOfWeek: 'MONDAY', ruleType: 'AVAILABLE', startTime: '09:00', endTime: '18:00' },
      { dayOfWeek: 'MONDAY', ruleType: 'BREAK', startTime: '13:00', endTime: '14:00' },
    ]);
    expect(component.success()).toContain('guardado');
  });

  it('creates and deletes a closed-date exception', () => {
    const saved = { id: 4, resourceId: 12, exceptionDate: component.today, exceptionType: 'CLOSED' as const, startTime: null, endTime: null, reason: 'Feriado', createdAt: '', updatedAt: '' };
    upsertScheduleException.mockReturnValue(of(saved));
    deleteScheduleException.mockReturnValue(of(undefined));
    component.exceptionForm.patchValue({ date: component.today, exceptionType: 'CLOSED', reason: ' Feriado ' });

    component.saveException();
    component.deleteException(saved);

    expect(upsertScheduleException).toHaveBeenCalledWith(7, 3, 12, component.today, { exceptionType: 'CLOSED', startTime: null, endTime: null, reason: 'Feriado' });
    expect(deleteScheduleException).toHaveBeenCalledWith(7, 3, 12, component.today);
    expect(component.exceptions()).toEqual([]);
  });
});
