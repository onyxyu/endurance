import { create } from 'zustand';

interface ConversationState {
  draft: string;
  setDraft: (draft: string) => void;
}

export const useConversationStore = create<ConversationState>((set) => ({
  draft: '',
  setDraft: (draft) => set({ draft }),
}));
