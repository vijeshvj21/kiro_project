import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import DirectionalIndicator from '../DirectionalIndicator';

/**
 * Tests for DirectionalIndicator component
 * Validates: Requirements 9.7
 */
describe('DirectionalIndicator', () => {
  describe('UP direction (specifiedTotal > precedingTotal)', () => {
    it('shows ↑ symbol', () => {
      render(<DirectionalIndicator specifiedTotal={200} precedingTotal={100} />);
      expect(screen.getByText('↑')).toBeInTheDocument();
    });

    it('shows "Increased" label', () => {
      render(<DirectionalIndicator specifiedTotal={200} precedingTotal={100} />);
      expect(screen.getByText('Increased')).toBeInTheDocument();
    });

    it('has green styling class', () => {
      const { container } = render(
        <DirectionalIndicator specifiedTotal={200} precedingTotal={100} />
      );
      const span = container.querySelector('span');
      expect(span.className).toContain('text-green-600');
    });

    it('has aria-label "Increased"', () => {
      render(<DirectionalIndicator specifiedTotal={200} precedingTotal={100} />);
      expect(screen.getByRole('generic', { name: 'Increased' })).toBeInTheDocument();
    });
  });

  describe('DOWN direction (specifiedTotal < precedingTotal)', () => {
    it('shows ↓ symbol', () => {
      render(<DirectionalIndicator specifiedTotal={50} precedingTotal={150} />);
      expect(screen.getByText('↓')).toBeInTheDocument();
    });

    it('shows "Decreased" label', () => {
      render(<DirectionalIndicator specifiedTotal={50} precedingTotal={150} />);
      expect(screen.getByText('Decreased')).toBeInTheDocument();
    });

    it('has red styling class', () => {
      const { container } = render(
        <DirectionalIndicator specifiedTotal={50} precedingTotal={150} />
      );
      const span = container.querySelector('span');
      expect(span.className).toContain('text-red-600');
    });

    it('has aria-label "Decreased"', () => {
      render(<DirectionalIndicator specifiedTotal={50} precedingTotal={150} />);
      expect(screen.getByRole('generic', { name: 'Decreased' })).toBeInTheDocument();
    });
  });

  describe('NEUTRAL direction (specifiedTotal === precedingTotal)', () => {
    it('shows → symbol', () => {
      render(<DirectionalIndicator specifiedTotal={100} precedingTotal={100} />);
      expect(screen.getByText('→')).toBeInTheDocument();
    });

    it('shows "No change" label', () => {
      render(<DirectionalIndicator specifiedTotal={100} precedingTotal={100} />);
      expect(screen.getByText('No change')).toBeInTheDocument();
    });

    it('has grey styling class', () => {
      const { container } = render(
        <DirectionalIndicator specifiedTotal={100} precedingTotal={100} />
      );
      const span = container.querySelector('span');
      expect(span.className).toContain('text-gray-500');
    });

    it('has aria-label "No change"', () => {
      render(<DirectionalIndicator specifiedTotal={100} precedingTotal={100} />);
      expect(screen.getByRole('generic', { name: 'No change' })).toBeInTheDocument();
    });
  });

  describe('string input handling', () => {
    it('correctly identifies UP when given string inputs', () => {
      render(<DirectionalIndicator specifiedTotal="300.50" precedingTotal="200.00" />);
      expect(screen.getByText('Increased')).toBeInTheDocument();
      expect(screen.getByText('↑')).toBeInTheDocument();
    });

    it('correctly identifies DOWN when given string inputs', () => {
      render(<DirectionalIndicator specifiedTotal="99.99" precedingTotal="150.00" />);
      expect(screen.getByText('Decreased')).toBeInTheDocument();
      expect(screen.getByText('↓')).toBeInTheDocument();
    });

    it('correctly identifies NEUTRAL when given equal string inputs', () => {
      render(<DirectionalIndicator specifiedTotal="75.25" precedingTotal="75.25" />);
      expect(screen.getByText('No change')).toBeInTheDocument();
      expect(screen.getByText('→')).toBeInTheDocument();
    });
  });
});
