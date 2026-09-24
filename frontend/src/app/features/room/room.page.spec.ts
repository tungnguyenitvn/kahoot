import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it } from 'vitest';
import { Room } from '../../shared/models/room';
import { RoomPage } from './room.page';
import { RoomStore } from './room.store';

/** The store as the template sees it: signals only, commands recorded. */
function fakeStore(room: Room) {
  const calls: string[] = [];
  return { calls, store: {
    room: signal<Room | null>(room), error: signal(''), connected: signal(true), accessDenied: signal(false), busy: signal(false),
    pendingAnswer: signal<null | object>(null), selection: signal<number | null>(null), remaining: signal(30),
    players: signal(Object.values(room.players)), activePlayers: signal(Object.values(room.players)), ranking: signal([]), host: signal(false),
    open: (id: string) => calls.push('open ' + id), answer: async () => { calls.push('answer'); }, refresh: async () => {}, control: async () => {},
  } };
}
const question: Room = {
  id: 'room-1', pin: '123456', title: 'Quiz', phase: 'QUESTION', version: 3, serverTime: 0, deadline: 0, questionNumber: 1, questionCount: 2,
  question: { roundId: 'r1', text: 'One?', options: ['a', 'b', 'c', 'd'], seconds: 30, correctOption: null },
  players: { p1: { id: 'p1', name: 'Ann', active: true, answered: false, score: 0 } },
  me: { role: 'PLAYER', participantId: 'p1', answer: null },
};

describe('room page', () => {
  it('ANSWER-01 an active player picks an option in the open question and sends it through the store', () => {
    const { calls, store } = fakeStore(question);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'room-1' })) } }] });
    TestBed.overrideComponent(RoomPage, { set: { providers: [{ provide: RoomStore, useValue: store }] } });
    const fixture = TestBed.createComponent(RoomPage);
    fixture.detectChanges();
    expect(calls).toEqual(['open room-1']);

    const options = fixture.nativeElement.querySelectorAll('button.answer') as NodeListOf<HTMLButtonElement>;
    const send = () => fixture.nativeElement.querySelector('button.primary') as HTMLButtonElement;
    expect(options.length).toBe(4);
    expect(send().disabled).toBe(true);

    options[1].click();
    fixture.detectChanges();
    expect(store.selection()).toBe(1);
    expect(options[1].classList.contains('selected')).toBe(true);
    expect(send().disabled).toBe(false);

    send().click();
    expect(calls).toEqual(['open room-1', 'answer']);
  });
});
