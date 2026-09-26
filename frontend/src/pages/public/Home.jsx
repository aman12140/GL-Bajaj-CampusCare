import { Link } from 'react-router-dom';
import { useAuth, HOME_BY_ROLE } from '../../context/AuthContext';

const FEATURES = [
  ['Structured reporting', 'Pick a category, then a campus location (building, floor), describe the problem and attach a photo. No vague complaints.'],
  ['Right department, right person', 'Every category suggests a department. The admin assigns an active staff member from it, with a clear reason if work is reassigned.'],
  ['Live status and history', 'Reported, Assigned, In Progress, Resolved, Closed. Every change is recorded with who did it and when.'],
  ['Student confirmation', 'An issue is closed only after the student confirms the fix. If it is not fixed, it can be reopened with a reason.'],
  ['Priority help without AI', 'A transparent keyword-based rule set suggests a priority and shows why. The student can always override it.'],
  ['Analytics and reports', 'Admins see real counts, resolution times and overdue issues, and can export the same data as CSV or PDF.'],
];
const STEPS = [
  ['Report', 'A student describes the issue, picks the location and submits it.'],
  ['Assign', 'The admin checks it and assigns the responsible department staff member.'],
  ['Fix', 'Staff start work and record what was done, optionally with a photo.'],
  ['Confirm', 'The student confirms the fix and can leave feedback, or reopens the issue.'],
];
const LOCATIONS = ['Block A', 'Block B', 'Cafeteria', 'Kalpana Chawla Hostel', 'APJ Abdul Kalam Hostel', 'Gym'];

export default function Home() {
  const { user } = useAuth();
  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <div className="hero-copy">
            <p className="eyebrow">Smart Campus Issue Management System</p>
            <h1>Report it. Track it. See it fixed.</h1>
            <p className="lead">
              GL Bajaj CampusCare gives students one place to report campus problems, and gives the administration and
              maintenance teams a clear queue to resolve them, with nothing lost along the way.
            </p>
            <div className="hero-actions">
              {user ? (
                <Link className="btn btn-primary btn-lg" to={HOME_BY_ROLE[user.role]}>Go to dashboard</Link>
              ) : (
                <>
                  <Link className="btn btn-primary btn-lg" to="/register">Student registration</Link>
                  <Link className="btn btn-secondary btn-lg" to="/login">Log in</Link>
                </>
              )}
            </div>
          </div>
          <aside className="hero-ticket" aria-label="Example of an issue timeline">
            <p className="ticket-tag">Example issue (illustration)</p>
            <h2>Wi-Fi not working near the lab</h2>
            <ol className="timeline">
              <li className="tl-item status-dot-reported"><div className="tl-head"><strong>Reported</strong></div><div className="tl-actor">Student</div></li>
              <li className="tl-item status-dot-assigned"><div className="tl-head"><strong>Assigned</strong></div><div className="tl-actor">Admin to IT &amp; Wi-Fi Support</div></li>
              <li className="tl-item status-dot-in_progress"><div className="tl-head"><strong>In progress</strong></div><div className="tl-actor">Staff started work</div></li>
              <li className="tl-item status-dot-resolved"><div className="tl-head"><strong>Resolved</strong></div><div className="tl-actor">Router replaced</div></li>
              <li className="tl-item status-dot-closed"><div className="tl-head"><strong>Closed</strong></div><div className="tl-actor">Student confirmed</div></li>
            </ol>
          </aside>
        </div>
      </section>

      <section id="about" className="section">
        <div className="section-inner narrow">
          <h2>About CampusCare</h2>
          <p>
            Campus problems such as a broken fan, a leaking tap or a dead network point often go unreported or get lost
            in phone calls and messages. CampusCare replaces that with a tracked workflow: every issue has an owner, a
            status, a history and a target time, so students know what is happening and the administration can see what needs attention.
          </p>
          <p>Locations available in this project:</p>
          <ul className="chip-list plain">{LOCATIONS.map((l) => <li key={l}>{l}</li>)}</ul>
        </div>
      </section>

      <section id="features" className="section alt">
        <div className="section-inner">
          <h2>What it does</h2>
          <dl className="feature-list">
            {FEATURES.map(([t, d]) => (<div key={t}><dt>{t}</dt><dd>{d}</dd></div>))}
          </dl>
        </div>
      </section>

      <section id="how-it-works" className="section">
        <div className="section-inner">
          <h2>How it works</h2>
          <ol className="steps">
            {STEPS.map(([t, d], i) => (<li key={t}><span className="step-no">{i + 1}</span><div><strong>{t}</strong><p>{d}</p></div></li>))}
          </ol>
        </div>
      </section>

      <section className="section alt">
        <div className="section-inner">
          <h2>Three roles, one system</h2>
          <div className="role-columns">
            <div><h3>Students</h3><p>Register, report issues with photos, follow progress, confirm fixes, reopen or rate.</p></div>
            <div><h3>Staff</h3><p>See only issues assigned to them, start work and record the resolution. Accounts are created by the admin.</p></div>
            <div><h3>Admin</h3><p>Assign and reject issues, manage staff, departments, categories and locations, and view analytics.</p></div>
          </div>
        </div>
      </section>

      <section id="contact" className="section">
        <div className="section-inner narrow">
          <h2>Contact</h2>
          <p>
            CampusCare is a college project and is not an official GL Bajaj Group of Institutions service. For questions
            about this project, please contact the project team or your project guide. Sign in to report a real issue only when the
            system has been deployed by your institute.
          </p>
        </div>
      </section>
    </>
  );
}
